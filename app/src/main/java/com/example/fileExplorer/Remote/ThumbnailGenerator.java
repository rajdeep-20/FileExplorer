package com.example.fileExplorer.Remote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;

/**
 * Utility for generating lightweight Base64 image thumbnails for remote synchronization.
 */
public class ThumbnailGenerator {

    private static final String TAG = "RFE:ThumbnailGen";
    private static final int DEFAULT_MAX_DIMENSION = 128;
    private static final int JPEG_QUALITY = 75;

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "bmp", "dng", "gif"
    );

    /**
     * Checks if a file is an image based on its extension or MIME type.
     */
    public static boolean isImage(String extension, String mimeType) {
        if (extension != null && SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            return true;
        }
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Generates a Base64-encoded JPEG thumbnail for the given file path.
     * Returns null if the file cannot be decoded or is not an image.
     */
    public static String generateThumbnailBase64(String filePath) {
        return generateThumbnailBase64(filePath, DEFAULT_MAX_DIMENSION);
    }

    /**
     * Generates a Base64-encoded JPEG thumbnail with a custom max dimension.
     */
    public static String generateThumbnailBase64(String filePath, int maxDimension) {
        if (filePath == null) return null;

        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || file.length() == 0) {
            return null;
        }

        Bitmap sampledBitmap = null;
        Bitmap scaledBitmap = null;
        ByteArrayOutputStream baos = null;

        try {
            // Step 1: Decode image dimensions without loading pixel data
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            int origWidth = options.outWidth;
            int origHeight = options.outHeight;
            if (origWidth <= 0 || origHeight <= 0) {
                return null;
            }

            // Step 2: Compute inSampleSize to downsample during file decoding
            int inSampleSize = 1;
            int largestDim = Math.max(origWidth, origHeight);
            while (largestDim / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2;
            }

            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Saves 50% memory over ARGB_8888

            sampledBitmap = BitmapFactory.decodeFile(filePath, options);
            if (sampledBitmap == null) {
                return null;
            }

            // Step 3: Exact scale down to fit within maxDimension preserving aspect ratio
            float scale = Math.min(
                    (float) maxDimension / sampledBitmap.getWidth(),
                    (float) maxDimension / sampledBitmap.getHeight()
            );

            int targetWidth = Math.max(1, Math.round(sampledBitmap.getWidth() * scale));
            int targetHeight = Math.max(1, Math.round(sampledBitmap.getHeight() * scale));

            if (sampledBitmap.getWidth() > targetWidth || sampledBitmap.getHeight() > targetHeight) {
                scaledBitmap = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true);
            } else {
                scaledBitmap = sampledBitmap;
            }

            // Step 4: Compress to JPEG byte array
            baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            byte[] imageBytes = baos.toByteArray();

            // Step 5: Encode to Base64 String
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        } catch (Throwable t) {
            Log.w(TAG, "Failed to generate thumbnail for " + filePath + ": " + t.getMessage());
            return null;
        } finally {
            if (sampledBitmap != null && sampledBitmap != scaledBitmap) {
                sampledBitmap.recycle();
            }
            if (scaledBitmap != null) {
                scaledBitmap.recycle();
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
