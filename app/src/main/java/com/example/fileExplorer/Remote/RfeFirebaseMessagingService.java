package com.example.fileExplorer.Remote;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class RfeFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RFE:FCMService";
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage){
        super.onMessageReceived(remoteMessage);


        Map<String, String> data = remoteMessage.getData();

        String type = data.get("type");
        Log.i(TAG, "FCM Message received Type" + type);


        if("CHECK_JOBS".equals(type)){
            Log.i(TAG, "CHECK_JOBS received --- enqueuing JobProcessingWorker");
            SyncScheduler.triggerImmediateJobCheck(getApplicationContext());
        }
        else {
            Log.w(TAG, "Unknown FCM Message type" + type);
        }
    }


    @Override
    public void onNewToken(@NonNull String token){
        super.onNewToken(token);

        Log.i(TAG, "FCM token refreshed " + token.substring(0,10) + ".....");
        DeviceIdentityManager.onTokenRefreshed(getApplicationContext(), token);
    }


}
