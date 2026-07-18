package com.example.fileexplorer.Remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;

import retrofit2.Response;

/**
 * WorkManager Worker that scans the device's key directories (Documents, Pictures,
 * Movies, Downloads) and bulk-syncs the file metadata to the Spring Boot backend.
 *
 * This worker runs:
 *  - On first app launch (triggered immediately by SyncScheduler)
 *  - Periodically every 6 hours (scheduled by SyncScheduler)
 *  - Only file metadata is sent — no actual file content is uploaded.
 */
public class MetadataSyncWorker extends Worker {

    private static final String TAG = "RFE:MetadataSync";

    /** Directories to scan on the device. */
    private static final String[] SCAN_ROOTS = {
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0/Downloads",
            "/storage/emulated/0/Music",
            "/storage/emulated/0/DCIM"
    };
    private static final Set<String> SCAN_ROOT_SET = new HashSet<>(Arrays.asList(SCAN_ROOTS));

    /** Maximum entries per API call to avoid request size limits. */
    private static final int BATCH_SIZE = 500;

    public MetadataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String deviceId = DeviceIdentityManager.getDeviceID(getApplicationContext());
        if (deviceId == null) {
            Log.w(TAG, "No device ID found. Skipping sync.");
            return Result.retry();
        }

        Log.i(TAG, "Starting metadata sync...");

        try {
            // Phase 1: Scan all target directories
            List<FileMetaDataDto> allMetadata = scanDirectories();
            Log.i(TAG, "Scanned " + allMetadata.size() + " files/directories");

            if (allMetadata.isEmpty()) {
                Log.i(TAG, "No files found to sync.");
                return Result.success();
            }

            // Phase 2: Upload in batches
            int totalBatches = (int) Math.ceil((double) allMetadata.size() / BATCH_SIZE);
            for (int i = 0; i < totalBatches; i++) {
                int start = i * BATCH_SIZE;
                int end = Math.min(start + BATCH_SIZE, allMetadata.size());
                List<FileMetaDataDto> batch = allMetadata.subList(start, end);

                Response<Map<String, Integer>> response = ApiClient.getApiService()
                        .syncMetaData(deviceId, batch)
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> stats = response.body();
                    Log.i(TAG, "Batch " + (i + 1) + "/" + totalBatches
                            + " — inserted: " + stats.getOrDefault("inserted", 0)
                            + ", updated: " + stats.getOrDefault("updated", 0)
                            + ", deleted: " + stats.getOrDefault("deleted", 0));
                } else {
                    Log.e(TAG, "Sync batch " + (i + 1) + " failed: HTTP " + response.code());
                    return Result.retry();
                }
            }

            // Phase 3: Heartbeat
            sendHeartbeat(deviceId);

            Log.i(TAG, "Metadata sync completed successfully.");
            return Result.success();

        } catch (IOException e) {
            Log.e(TAG, "Sync failed due to network error. Will retry.", e);
            return Result.retry();
        }
    }

    /**
     * Walks the target directories and builds a flat list of FileMetadataDto entries.
     * Skips hidden directories (starting with '.').
     */
    private List<FileMetaDataDto> scanDirectories() {
        List<FileMetaDataDto> result = new ArrayList<>();

        for (String rootPath : SCAN_ROOTS) {
            Path root = Paths.get(rootPath);
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                Log.d(TAG, "Skipping non-existent directory: " + rootPath);
                continue;
            }

            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        // Skip hidden directories
                        if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        // Add the directory itself as metadata
                        result.add(new FileMetaDataDto(
                                null,
                                null,
                                dir.toAbsolutePath().toString(),
                                getParentPathForSync(dir),
                                dir.getFileName() != null ? dir.getFileName().toString() : "",
                                0L,
                                attrs.lastModifiedTime().toMillis(),
                                true
                        ));

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        result.add(new FileMetaDataDto(
                                null,
                                null,
                                file.toAbsolutePath().toString(),
                                getParentPathForSync(file),
                                file.getFileName().toString(),
                                attrs.size(),
                                attrs.lastModifiedTime().toMillis(),
                                false
                        ));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        Log.d(TAG, "Could not visit: " + file + " (" + exc.getMessage() + ")");
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error scanning directory: " + rootPath, e);
            }
        }

        return result;
    }

    private String getParentPathForSync(Path path) {
        String normalizedPath = normalizePath(path);
        if (SCAN_ROOT_SET.contains(normalizedPath)) {
            return "/";
        }

        Path parent = path.getParent();
        return parent == null ? "/" : normalizePath(parent);
    }

    private String normalizePath(Path path) {
        return path.toAbsolutePath().toString().replace("\\", "/");
    }

    /**
     * Sends a heartbeat to update the device's lastSeen on the backend.
     */
    @SneakyThrows
    private void sendHeartbeat(String deviceId) {
        ApiClient.getApiService().heartbeat(Map.of("deviceID", deviceId)).execute();
        Log.d(TAG, "Heartbeat sent.");
    }
}
