package com.example.fileExplorer.Remote;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeviceDto {
    private String deviceID;
    private String deviceName;
    private String FCMToken;
    private Instant lastSeen;
    private String status;



}
