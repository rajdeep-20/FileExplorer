package com.example.fileexplorer.Remote;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class JobProcessorWorker extends Worker {

    private static final String TAG = "RFE:JobProcessor";
    public JobProcessorWorker(@NonNull Context context, @NonNull WorkerParameters parameters)
    {
        super(context,parameters);
    }
    @NonNull
    @Override
    public Result doWork() {
        // Acquire WakeLock + WifiLock to keep CPU and network alive with screen off
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "RFE:JobProcessorWakeLock");
        wakeLock.acquire(30 * 60 * 1000L); // 30 min max safety timeout

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
        if(deviceID == null)
        {
            Log.w(TAG, "No device ID found. Skipping Job Processing");
            return Result.retry();
        }
        Log.i(TAG, "Starting to Process the Job");

        try {
            while (true) {
                if (isStopped()) {
                    Log.i(TAG, "Worker Stopped by system. Exiting Loop");
                    return Result.success();
                }

                Response<JobDto> claimResponse = ApiClient.getApiService()
                        .claimNextJobs(deviceID)
                        .execute();

                if (claimResponse.code() == 204 || claimResponse.body() == null) {
                    Log.i(TAG, "No more Pending Jobs");
                    break;
                }

                if(!claimResponse.isSuccessful()){
                    Log.e(TAG, "Failed to claim job : HTTP" + claimResponse.code());
                    return Result.retry();
                }
                JobDto job = claimResponse.body();
                Log.i(TAG, "Claimed Job" + job.getJobID() + "Type : " + job.getType() + "Payload" + job.getPayload());

                processJob(job);

            }
            Log.i(TAG, "All Jobs Processed");
            return Result.success();


        } catch (Exception e) {
            Log.e(TAG, "Error during job processing", e);
            return Result.retry();
        }
    }

    private void processJob(JobDto jobDto) throws IOException{
        String type = jobDto.getType();

        if("DOWNLOAD".equals(type))
        {
            processDownloadJob(jobDto);
        }
        else if ("UPLOAD".equals(type)) 
        {
            processUploadJob(jobDto);
        }
        else {
            Log.w(TAG, "Unknown Job type: " + type + ". Marking as failed.");
            reportJobFailed(jobDto.getJobID(), "Unsupported Job type" + type);
        }
    }

    private void processDownloadJob(JobDto jobDto) throws IOException{
        String  filePath = jobDto.getPayload();
        File file = new File(filePath);

        if(!file.exists()){
            Log.e(TAG, "File not found" + filePath);
            reportJobFailed(jobDto.getJobID(), "File not found on the device" + filePath);
            return;
        }
        if(!file.canRead())
        {
            Log.e(TAG, "Cannot Read file" + filePath);
            reportJobFailed(jobDto.getJobID(), "No read permission for the file"  + filePath);
            return;
        }
        if(file.isDirectory()){
            Log.e(TAG, "Cannot Upload directory");
            reportJobFailed(jobDto.getJobID(), "Path is a directory, not a file: " + filePath);
            return;
        }

        Log.i(TAG, "Uploading the file" + filePath + "(" + file.length() + "bytes");

        RequestBody requestBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestBody);


        Response<Void> uploadResponse = ApiClient.getApiService().uploadFile(jobDto.getJobID(), filePart).execute();


        if(!uploadResponse.isSuccessful()){
            Log.i(TAG,"Upload Http " + uploadResponse.code());
            reportJobFailed(jobDto.getJobID(), "Upload failed with Http: " + uploadResponse.code());
        }
    }
    
    private void processUploadJob(JobDto jobDto) {
        String targetPath = jobDto.getPayload();
        File file = new File(targetPath);
        
        try {
            Response<okhttp3.ResponseBody> response = ApiClient.getApiService().downloadFile(jobDto.getJobID()).execute();
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Download Http " + response.code());
                reportJobFailed(jobDto.getJobID(), "Download failed with Http: " + response.code());
                return;
            }
            
            // Create parent directories if they don't exist
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            Log.i(TAG, "Successfully downloaded file to " + targetPath);
            ApiClient.getApiService()
                .updateJobStatus(jobDto.getJobID(), Map.of("status", "COMPLETED"))
                .execute();
                
        } catch (IOException e) {
            Log.e(TAG, "Error saving downloaded file", e);
            reportJobFailed(jobDto.getJobID(), "Error saving file: " + e.getMessage());
        }
    }

    @SneakyThrows
    private void reportJobFailed(String jobID, String errorMessage){
        ApiClient.getApiService()
                .updateJobStatus(jobID, Map.of("status", "FAILED", "errorMessage", errorMessage))
                .execute();

        Log.i(TAG, "Job" + jobID + "marked as Failed" + errorMessage);
    }
}

