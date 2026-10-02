package com.example.fileExplorer.Remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fileExplorer.FileUtils;

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
 * WorkManager Worker that scans the device's key directories and bulk-syncs
 * file metadata to the Spring Boot backend.
 *
 * Runs on first app launch (immediate) and periodically every 6 hours.
 * Only metadata is sent — no actual file content is uploaded.
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

    private static final int METADATA_BATCH_SIZE = 500;
    private static final int THUMBNAIL_BATCH_SIZE = 50;

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

            // Phase 2: Upload basic metadata in batches
            if (!uploadInBatches(deviceId, allMetadata, METADATA_BATCH_SIZE, "Metadata")) {
                return Result.retry();
            }

            // Phase 3: Generate and upload thumbnails for image files
            uploadThumbnails(deviceId, allMetadata);

            // Phase 4: Heartbeat
            sendHeartbeat(deviceId);

            Log.i(TAG, "Metadata and thumbnail sync completed successfully.");
            return Result.success();

        } catch (IOException e) {
            Log.e(TAG, "Sync failed due to network error. Will retry.", e);
            return Result.retry();
        }
    }

    /** Uploads a list of DTOs in batches. Returns false if any batch fails. */
    private boolean uploadInBatches(String deviceId, List<FileMetaDataDto> items, int batchSize, String label) throws IOException {
        int totalBatches = (int) Math.ceil((double) items.size() / batchSize);
        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, items.size());
            List<FileMetaDataDto> batch = items.subList(start, end);

            Response<Map<String, Integer>> response = ApiClient.getApiService()
                    .syncMetaData(deviceId, batch).execute();

            if (response.isSuccessful() && response.body() != null) {
                Map<String, Integer> stats = response.body();
                Log.i(TAG, label + " batch " + (i + 1) + "/" + totalBatches
                        + " — inserted: " + stats.getOrDefault("inserted", 0)
                        + ", updated: " + stats.getOrDefault("updated", 0)
                        + ", deleted: " + stats.getOrDefault("deleted", 0));
            } else {
                Log.e(TAG, label + " batch " + (i + 1) + " failed: HTTP " + response.code());
                return false;
            }
        }
        return true;
    }

    /** Extracts image files, generates thumbnails, and uploads in batches. */
    private void uploadThumbnails(String deviceId, List<FileMetaDataDto> allMetadata) throws IOException {
        List<FileMetaDataDto> imageFiles = new ArrayList<>();
        for (FileMetaDataDto item : allMetadata) {
            if (!Boolean.TRUE.equals(item.getIsDirectory()) && ThumbnailGenerator.isImage(item.getExtension(), item.getMimeType())) {
                imageFiles.add(item);
            }
        }

        Log.i(TAG, "Processing thumbnails for " + imageFiles.size() + " images...");
        int totalBatches = (int) Math.ceil((double) imageFiles.size() / THUMBNAIL_BATCH_SIZE);
        for (int i = 0; i < totalBatches; i++) {
            int start = i * THUMBNAIL_BATCH_SIZE;
            int end = Math.min(start + THUMBNAIL_BATCH_SIZE, imageFiles.size());
            List<FileMetaDataDto> thumbBatch = new ArrayList<>();

            for (int j = start; j < end; j++) {
                FileMetaDataDto original = imageFiles.get(j);
                String thumbBase64 = ThumbnailGenerator.generateThumbnailBase64(original.getPath());
                if (thumbBase64 != null) {
                    FileMetaDataDto thumbDto = new FileMetaDataDto(
                            original.getId(), original.getDeviceID(), original.getPath(),
                            original.getParentPath(), original.getName(), original.getSize(),
                            original.getLastModified(), original.getIsDirectory(),
                            original.getMimeType(), original.getExtension(), thumbBase64);
                    thumbBatch.add(thumbDto);
                }
            }

            if (!thumbBatch.isEmpty()) {
                Response<Map<String, Integer>> resp = ApiClient.getApiService()
                        .syncMetaData(deviceId, thumbBatch).execute();
                if (resp.isSuccessful()) {
                    Log.i(TAG, "Thumbnail batch " + (i + 1) + "/" + totalBatches + " uploaded (" + thumbBatch.size() + " thumbnails)");
                } else {
                    Log.w(TAG, "Thumbnail batch " + (i + 1) + " failed: HTTP " + resp.code());
                }
            }
        }
    }

    /**
     * Walks the target directories and builds a flat list of FileMetaDataDto entries.
     * Uses FileMetaDataDto.fromPath() factory for each entry.
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
                        if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        try {
                            result.add(FileMetaDataDto.fromPath(dir, getParentPathForSync(dir)));
                        } catch (IOException ignored) {}
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            result.add(FileMetaDataDto.fromPath(file, getParentPathForSync(file)));
                        } catch (IOException ignored) {}
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
        String normalizedPath = path.toAbsolutePath().toString().replace("\\", "/");
        if (SCAN_ROOT_SET.contains(normalizedPath)) return "/";
        Path parent = path.getParent();
        return parent == null ? "/" : parent.toAbsolutePath().toString().replace("\\", "/");
    }

    @SneakyThrows
    private void sendHeartbeat(String deviceId) {
        ApiClient.getApiService().heartbeat(Map.of("deviceID", deviceId)).execute();
        Log.d(TAG, "Heartbeat sent.");
    }
}
