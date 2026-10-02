package com.example.fileExplorer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles asynchronous thumbnail loading for file list items.
 * Supports native media (via Glide), PDF first-page rendering, and APK icon extraction.
 */
public class ThumbnailLoader {

    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ThumbnailLoader(Context context) {
        this.context = context;
    }

    /**
     * Loads the appropriate thumbnail into the ImageView based on the file type.
     * Clears any stale Glide bitmaps from recycled views first.
     */
    public void loadThumbnail(ImageView imageView, FileItem item, FileTypeRegistry type, int fallbackIcon, int accentColor) {
        Glide.with(context).clear(imageView);

        if (type.isMediaPreview()) {
            loadMediaThumbnail(imageView, item, fallbackIcon, accentColor);
        } else if (type == FileTypeRegistry.PDF) {
            loadPdfThumbnail(imageView, item, fallbackIcon);
        } else if (type == FileTypeRegistry.APK) {
            loadApkIcon(imageView, item, fallbackIcon);
        }
    }

    private void loadMediaThumbnail(ImageView imageView, FileItem item, int fallbackIcon, int accentColor) {
        Glide.with(context)
                .load(new File(item.getAbsolutePath()))
                .placeholder(fallbackIcon)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        imageView.setColorFilter(accentColor);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imageView.clearColorFilter();
                        return false;
                    }
                })
                .into(imageView);
    }

    private void loadPdfThumbnail(ImageView imageView, FileItem item, int fallbackIcon) {
        executor.execute(() -> {
            Bitmap thumb = renderPdfThumbnail(item.getAbsolutePath());
            mainHandler.post(() -> {
                if (thumb != null) {
                    Glide.with(context).load(thumb).centerCrop().into(imageView);
                    imageView.clearColorFilter();
                } else {
                    imageView.setImageResource(fallbackIcon);
                }
            });
        });
    }

    private void loadApkIcon(ImageView imageView, FileItem item, int fallbackIcon) {
        executor.execute(() -> {
            Drawable apkIcon = extractApkIcon(item.getAbsolutePath());
            mainHandler.post(() -> {
                if (apkIcon != null) {
                    Glide.with(context).load(apkIcon).centerCrop().into(imageView);
                    imageView.clearColorFilter();
                } else {
                    imageView.setImageResource(fallbackIcon);
                }
            });
        });
    }

    /** Renders the first page of a PDF file into a Bitmap thumbnail. */
    private Bitmap renderPdfThumbnail(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) return null;
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            PdfRenderer.Page page = renderer.openPage(0);
            int width = 512;
            int height = (int) (width * ((float) page.getHeight() / page.getWidth()));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(android.graphics.Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
            pfd.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /** Extracts the launcher icon from an APK file using PackageManager. */
    private Drawable extractApkIcon(String apkPath) {
        try {
            PackageManager pm = context.getPackageManager();
            android.content.pm.PackageInfo pi = pm.getPackageArchiveInfo(apkPath, 0);
            if (pi == null) return null;
            ApplicationInfo ai = pi.applicationInfo;
            ai.sourceDir = apkPath;
            ai.publicSourceDir = apkPath;
            return ai.loadIcon(pm);
        } catch (Exception e) {
            return null;
        }
    }
}
