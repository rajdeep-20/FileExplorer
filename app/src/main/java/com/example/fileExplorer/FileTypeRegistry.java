package com.example.fileExplorer;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for file type detection.
 * Maps file extensions to icons, colors, MIME types, and categories.
 */
public enum FileTypeRegistry {

    FOLDER   (R.drawable.ic_folder,  R.color.color_folder,  "inode/directory", null,        true),
    IMAGE    (R.drawable.ic_image,   R.color.color_image,    "image/jpeg",     "images",    true,  "jpg", "jpeg", "png", "raw", "dng", "webp", "gif", "bmp"),
    PDF      (R.drawable.ic_pdf,     R.color.color_pdf,      "application/pdf","documents", false, "pdf"),
    AUDIO    (R.drawable.ic_music,   R.color.color_audio,    "audio/x-wav",    "music",     false, "mp3", "wav", "m4a", "ogg"),
    VIDEO    (R.drawable.ic_play,    R.color.color_video,    "video/*",        "video",     true,  "mp4", "mkv", "avi", "3gp"),
    DOC      (R.drawable.ic_docs,    R.color.color_doc,      "application/msword", "documents", false, "doc", "docx"),
    TEXT     (R.drawable.ic_docs,    R.color.color_txt,      "text/plain",     "documents", false, "txt"),
    SPREADSHEET(R.drawable.ic_docs,  R.color.color_doc,      "application/vnd.ms-excel", "documents", false, "xls", "xlsx"),
    PRESENTATION(R.drawable.ic_docs, R.color.color_doc,      "application/vnd.ms-powerpoint", "documents", false, "ppt", "pptx"),
    APK      (R.drawable.ic_android, R.color.color_apk,      "application/vnd.android.package-archive", "APK", false, "apk"),
    GENERIC  (R.drawable.ic_folder,  R.color.color_generic,  "*/*",            null,        false);

    private final int iconRes;
    private final int colorRes;
    private final String mimeType;
    private final String category;
    private final boolean isMediaPreview;
    private final Set<String> extensions;

    private static final Map<String, FileTypeRegistry> EXT_MAP = new HashMap<>();

    static {
        for (FileTypeRegistry type : values()) {
            for (String ext : type.extensions) {
                EXT_MAP.put(ext, type);
            }
        }
    }

    FileTypeRegistry(int iconRes, int colorRes, String mimeType, String category, boolean isMediaPreview, String... extensions) {
        this.iconRes = iconRes;
        this.colorRes = colorRes;
        this.mimeType = mimeType;
        this.category = category;
        this.isMediaPreview = isMediaPreview;
        this.extensions = new HashSet<>(Arrays.asList(extensions));
    }

    public int getIconRes() { return iconRes; }
    public int getAccentColor(Context ctx) { return ContextCompat.getColor(ctx, colorRes); }
    public String getMimeType() { return mimeType; }
    public String getCategory() { return category; }

    /** True for image/video types that Glide can preview natively. */
    public boolean isMediaPreview() { return isMediaPreview; }

    /** Resolve a FileTypeRegistry entry from a filename. */
    public static FileTypeRegistry fromFileName(String fileName) {
        if (fileName == null) return GENERIC;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return GENERIC;
        String ext = fileName.substring(dot + 1).toLowerCase();
        return EXT_MAP.getOrDefault(ext, GENERIC);
    }

    /** Check if a filename belongs to the given category (e.g. "images", "video"). */
    public static boolean matchesCategory(String fileName, String categoryName) {
        if ("downloads".equals(categoryName)) return true;
        FileTypeRegistry type = fromFileName(fileName);
        return categoryName.equals(type.category);
    }
}
