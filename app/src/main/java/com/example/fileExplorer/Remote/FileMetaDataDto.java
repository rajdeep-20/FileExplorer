package com.example.fileExplorer.Remote;

import com.example.fileExplorer.FileUtils;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMetaDataDto {
    private String id;
    private String deviceID;
    private String path;
    private String parentPath;
    private String name;
    private Long size;
    private Long lastModified;
    @SerializedName("isDirectory")
    private Boolean isDirectory;
    private String mimeType;
    private String extension;
    private String thumbnail;

    public FileMetaDataDto(String id, String deviceID, String path, String parentPath, String name,
                           Long size, Long lastModified, Boolean isDirectory, String mimeType, String extension) {
        this(id, deviceID, path, parentPath, name, size, lastModified, isDirectory, mimeType, extension, null);
    }

    /**
     * Factory: builds a DTO from a java.nio.file.Path by reading its attributes.
     * Handles extension, MIME type, parent path normalization, and optional thumbnail generation.
     */
    public static FileMetaDataDto fromPath(Path entry, String parentPathOverride, boolean includeThumbnail) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
        boolean isDir = attrs.isDirectory();
        String name = entry.getFileName().toString();
        String ext = isDir ? "" : FileUtils.getExtension(name);
        String mime = isDir ? "inode/directory" : FileUtils.getMimeType(ext);
        String parent = parentPathOverride != null
                ? parentPathOverride.replace("\\", "/")
                : (entry.getParent() != null ? entry.getParent().toAbsolutePath().toString().replace("\\", "/") : "/");

        String thumb = null;
        if (includeThumbnail && !isDir && ThumbnailGenerator.isImage(ext, mime)) {
            thumb = ThumbnailGenerator.generateThumbnailBase64(entry.toAbsolutePath().toString());
        }

        return new FileMetaDataDto(
                null, null,
                entry.toAbsolutePath().toString(),
                parent, name,
                isDir ? 0L : attrs.size(),
                attrs.lastModifiedTime().toMillis(),
                isDir, mime, ext, thumb
        );
    }

    /** Convenience: fromPath without thumbnail. */
    public static FileMetaDataDto fromPath(Path entry, String parentPathOverride) throws IOException {
        return fromPath(entry, parentPathOverride, false);
    }
}
