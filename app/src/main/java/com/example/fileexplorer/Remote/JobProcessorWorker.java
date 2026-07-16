package com.example.fileexplorer.Remote;

import android.app.DownloadManager;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;
import android.view.PixelCopy;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

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


        if(uploadResponse.isSuccessful()){
            Log.i(TAG,"Upload Http " + uploadResponse.code());
            reportJobFailed(jobDto.getJobID(), "Upload failed with Http: " + uploadResponse.code());
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
