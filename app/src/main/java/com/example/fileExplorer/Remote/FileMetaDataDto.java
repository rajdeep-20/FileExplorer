package com.example.fileexplorer.Remote;

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
    private Boolean isDirectory;
}
