package com.example.fileexplorer.Remote;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface RfeApiService {

    @POST("api/v1/devices/register")
    Call<DeviceDto> registerDevice(@Body Map<String, String> payload);

    @POST("api/v1/devices/heartbeat")
    Call<Void> heartbeat(@Body Map<String, String> payload);

    @POST("api/v1/sync/metadata")
    Call<Map<String, Integer>> syncMetaData(@Header("X-Device-ID") String deviceID, @Body List<FileMetaDataDto> metaDataDtoList);

    @POST("api/v1/sync/delta/upsert")
    Call<Void> syncDeltaUpsert(@Header("X-Device-ID") String deviceID, @Body List<FileMetaDataDto> deltaList);

    @POST("api/v1/sync/delta/delete")
    Call<Void> syncDeltaDelete(@Header("X-Device-ID") String deviceID, @Header("X-File-Path") String path);

    @GET("api/v1/jobs/pending")
    Call<List<JobDto>> getPendingJobs(@Header("X-Device-ID") String deviceID);

    @POST("api/v1/jobs/claim")
    Call<JobDto> claimNextJobs(@Header("X-Device-ID") String deviceID);

    @POST("api/v1/jobs/{jobID}/complete")
    Call<Void> updateJobStatus(
            @Path("jobID") String jobID,
            @Body Map<String, String> payload
    );

    @Multipart
    @POST("api/v1/jobs/upload/{jobID}")
    Call<Void> uploadFile(@Path("jobID") String jobID, @Part MultipartBody.Part file);
    
    @GET("api/v1/jobs/download/{jobID}")
    Call<okhttp3.ResponseBody> downloadFile(@Path("jobID") String jobID);
}
