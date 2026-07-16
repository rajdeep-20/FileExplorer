package com.example.fileexplorer.Remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import retrofit2.Response;

@UtilityClass
public class DeviceIdentityManager {
    private static final String TAG = "RFE:DeviceIdentity";
    private static final String PREFS_NAME = "rfe_prefs_name";
    private static final String KEY_DEVICE_ID = "device_ID";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_REGISTERED = "is_registered";

    public static void initialize(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String deviceID = sharedPreferences.getString(KEY_DEVICE_ID, null);
        if(deviceID == null)
        {
            deviceID = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_DEVICE_ID, deviceID).apply();
            Log.i(TAG, "Generated new Device ID " + deviceID);
        }

        final String finalDeviceID = deviceID;

        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.w(TAG, "Firebase is not initialized. Skipping token retrieval. (Check google-services.json)");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token ->{
                    Log.i(TAG, "FCM Token retrived" + token.substring(0, 10) + "........");
                    String savedToken = sharedPreferences.getString(KEY_FCM_TOKEN, null);
                    boolean isRegistered  = sharedPreferences.getBoolean(KEY_REGISTERED, false);

                    if(!isRegistered || !token.equals(savedToken)){
                        registerWithBackend(context, finalDeviceID, token);
                    }
                    else {
                        Log.d(TAG, "Already Registered, token unchanged, Skipping");
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "failed to get FCM Token", e));
    }
    public static void onTokenRefreshed(Context context, String newToken){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String deviceID = prefs.getString(KEY_DEVICE_ID, null);
        if(deviceID != null){
            registerWithBackend(context, deviceID, newToken);
        }
        else {
            Log.w(TAG, "Token refreshed but no device ID exists yet, Will register on next app launch.");
        }
    }



    public static String getDeviceID(Context context){
            return context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE).getString(KEY_DEVICE_ID, null);
    }



    private static void registerWithBackend(Context context, String deviceID, String FCMToken){
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("deviceID", deviceID);
                payload.put("deviceName", Build.MODEL);
                payload.put("FCMToken", FCMToken);

                Response<DeviceDto> response = ApiClient.getApiService()
                        .registerDevice(payload)
                        .execute();

                if (response.isSuccessful()) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_REGISTERED, true).putString(KEY_FCM_TOKEN, FCMToken).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Registration failed", e);
            }
        }).start();
    }
}
