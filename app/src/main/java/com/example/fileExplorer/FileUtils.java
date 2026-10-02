package com.example.fileExplorer;

import android.webkit.MimeTypeMap;

/**
 * Shared file utility methods. Eliminates copy-pasted helpers
 * across DeltaSyncManager, JobProcessorWorker, and MetadataSyncWorker.
 */
public final class FileUtils {

    private FileUtils() {} // Non-instantiable

    /**
     * Extracts the lowercase file extension from a filename.
     * Returns empty string for files without an extension.
     * e.g. "photo.JPG" → "jpg", "README" → ""
     */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase();
    }

    /**
     * Resolves a MIME type from a file extension using Android's MimeTypeMap.
     * Falls back to "application/octet-stream" for unknown or empty extensions.
     */
    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) return "application/octet-stream";
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mime != null ? mime : "application/octet-stream";
    }
}
