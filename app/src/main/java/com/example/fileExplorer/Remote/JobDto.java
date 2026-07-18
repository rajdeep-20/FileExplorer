package com.example.fileExplorer.Remote;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JobDto {
    private String jobID;
    private String deviceID;
    private String type;
    private String status;
    private String payload;
    private String errorMessage;
    private String resultPayload;
    private Instant createdAt;
}
