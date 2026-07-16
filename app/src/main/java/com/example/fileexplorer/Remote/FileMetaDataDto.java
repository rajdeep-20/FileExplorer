package com.example.fileexplorer.Remote;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMetaDataDto {
    private String id;
    private String deviceID;
    private String name;
    private String path;
    private Long size;
    private Instant modifiedDate;
    private Boolean isDirectory;
}
