package com.example.fileExplorer.Remote;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fileExplorer.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class JobProcessorWorker extends Worker {

    private static final String TAG = "RFE:JobProcessor";
    private static final int BATCH_SIZE = 50;

    public JobProcessorWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "RFE:JobProcessorWakeLock");
        wakeLock.acquire(30 * 60 * 1000L);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = wm.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF, "RFE:JobProcessorWifiLock");
        wifiLock.acquire();

        try {
            return doJobProcessing();
        } finally {
            if (wifiLock.isHeld()) wifiLock.release();
            if (wakeLock.isHeld()) wakeLock.release();
            Log.i(TAG, "Released WakeLock and WifiLock");
        }
    }

    private Result doJobProcessing() {
        String deviceID = DeviceIdentityManager.getDeviceID(getApplicationContext());
        if (deviceID == null) {
            Log.w(TAG, "No device ID found. Skipping Job Processing");
            return Result.retry();
        }
        Log.i(TAG, "Starting to Process Jobs");

        try {
            while (true) {
                if (isStopped()) {
                    Log.i(TAG, "Worker Stopped by system. Exiting Loop");
                    return Result.success();
                }

                Response<JobDto> claimResponse = ApiClient.getApiService()
                        .claimNextJobs(deviceID).execute();

                if (claimResponse.code() == 204 || claimResponse.body() == null) {
                    Log.i(TAG, "No more Pending Jobs");
                    break;
                }
                if (!claimResponse.isSuccessful()) {
                    Log.e(TAG, "Failed to claim job: HTTP " + claimResponse.code());
                    return Result.retry();
                }

                JobDto job = claimResponse.body();
                Log.i(TAG, "Claimed Job " + job.getJobID() + " Type: " + job.getType());
                processJob(job);
            }
            Log.i(TAG, "All Jobs Processed");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error during job processing", e);
            return Result.retry();
        }
    }

    private void processJob(JobDto jobDto) throws IOException {
        switch (jobDto.getType()) {
            case "DOWNLOAD":    processDownloadJob(jobDto); break;
            case "UPLOAD":      processUploadJob(jobDto); break;
            case "REFRESH_DIR": processRefreshDirJob(jobDto); break;
            default:
                Log.w(TAG, "Unknown Job type: " + jobDto.getType());
                reportJobFailed(jobDto.getJobID(), "Unsupported Job type: " + jobDto.getType());
        }
    }

    private void processDownloadJob(JobDto jobDto) throws IOException {
        String filePath = jobDto.getPayload();
        File file = new File(filePath);

        if (!file.exists()) {
            reportJobFailed(jobDto.getJobID(), "File not found: " + filePath); return;
        }
        if (!file.canRead()) {
            reportJobFailed(jobDto.getJobID(), "No read permission: " + filePath); return;
        }
        if (file.isDirectory()) {
            reportJobFailed(jobDto.getJobID(), "Path is a directory: " + filePath); return;
        }

        Log.i(TAG, "Uploading " + filePath + " (" + file.length() + " bytes)");
        RequestBody body = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), body);

        Response<Void> resp = ApiClient.getApiService().uploadFile(jobDto.getJobID(), filePart).execute();
        if (!resp.isSuccessful()) {
            reportJobFailed(jobDto.getJobID(), "Upload failed: HTTP " + resp.code());
        }
    }

    private void processUploadJob(JobDto jobDto) {
        String targetPath = jobDto.getPayload();
        File file = new File(targetPath);

        try {
            Response<okhttp3.ResponseBody> response = ApiClient.getApiService().downloadFile(jobDto.getJobID()).execute();
            if (!response.isSuccessful() || response.body() == null) {
                reportJobFailed(jobDto.getJobID(), "Download failed: HTTP " + response.code());
                return;
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            Log.i(TAG, "Downloaded file to " + targetPath);
            ApiClient.getApiService()
                    .updateJobStatus(jobDto.getJobID(), Map.of("status", "COMPLETED"))
                    .execute();
        } catch (IOException e) {
            Log.e(TAG, "Error saving downloaded file", e);
            reportJobFailed(jobDto.getJobID(), "Error saving file: " + e.getMessage());
        }
    }

    /**
     * Handles a REFRESH_DIR job: scans the requested directory (non-recursive),
     * pushes fresh metadata to the backend using FileMetaDataDto.fromPath().
     */
    private void processRefreshDirJob(JobDto jobDto) throws IOException {
        String dirPath = jobDto.getPayload();
        String deviceID = DeviceIdentityManager.getDeviceID(getApplicationContext());

        if (dirPath == null || dirPath.isBlank()) {
            reportJobFailed(jobDto.getJobID(), "REFRESH_DIR job has no path payload");
            return;
        }

        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            reportJobFailed(jobDto.getJobID(), "Directory not found: " + dirPath);
            return;
        }

        Log.i(TAG, "REFRESH_DIR: scanning " + dirPath);
        List<FileMetaDataDto> batch = new ArrayList<>();
        String parentPath = dirPath.replace("\\", "/");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                batch.add(FileMetaDataDto.fromPath(entry, parentPath, true));
            }
        } catch (AccessDeniedException e) {
            reportJobFailed(jobDto.getJobID(), "Access Denied: " + dirPath);
            return;
        }

        // Upload in batches
        if (!batch.isEmpty()) {
            int totalBatches = (int) Math.ceil((double) batch.size() / BATCH_SIZE);
            for (int i = 0; i < totalBatches; i++) {
                int start = i * BATCH_SIZE;
                int end = Math.min(start + BATCH_SIZE, batch.size());
                Response<Map<String, Integer>> syncResp =
                        ApiClient.getApiService().syncMetaData(deviceID, batch.subList(start, end)).execute();
                if (!syncResp.isSuccessful()) {
                    reportJobFailed(jobDto.getJobID(), "Metadata sync failed: HTTP " + syncResp.code());
                    return;
                }
            }
            Log.i(TAG, "REFRESH_DIR: synced " + batch.size() + " entries for " + dirPath);
        }

        ApiClient.getApiService()
                .updateJobStatus(jobDto.getJobID(), Map.of("status", "COMPLETED"))
                .execute();
        Log.i(TAG, "REFRESH_DIR job " + jobDto.getJobID() + " completed.");
    }

    @SneakyThrows
    private void reportJobFailed(String jobID, String errorMessage) {
        ApiClient.getApiService()
                .updateJobStatus(jobID, Map.of("status", "FAILED", "errorMessage", errorMessage))
                .execute();
        Log.i(TAG, "Job " + jobID + " marked as Failed: " + errorMessage);
    }
}
