package com.example.fileExplorer.Remote;

import com.google.gson.annotations.SerializedName;

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
}
