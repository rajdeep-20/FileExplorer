package com.example.fileExplorer.Remote;

import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import com.example.fileExplorer.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class DeltaSyncManager {
    private static final String TAG = "RFE:DeltaSync";
    private static DeltaSyncManager instance;
    private final Context context;
    private final Map<String, FileObserver> observers = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String[] SCAN_ROOTS = {
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0/Downloads",
            "/storage/emulated/0/Music",
            "/storage/emulated/0/DCIM"
    };

    private DeltaSyncManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized DeltaSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeltaSyncManager(context);
        }
        return instance;
    }

    public void start() {
        if (!observers.isEmpty()) return; // Already running
        Log.i(TAG, "Starting Hybrid Delta Sync Observers...");
        for (String root : SCAN_ROOTS) {
            startObserving(root);
        }
    }

    public void stop() {
        Log.i(TAG, "Stopping Hybrid Delta Sync Observers...");
        for (FileObserver observer : observers.values()) {
            observer.stopWatching();
        }
        observers.clear();
    }

    @SuppressWarnings("deprecation")
    private void startObserving(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) return;
        if (observers.containsKey(path)) return;

        int mask = FileObserver.CREATE | FileObserver.DELETE | FileObserver.MODIFY | FileObserver.MOVED_TO | FileObserver.MOVED_FROM;
        FileObserver observer = new FileObserver(path, mask) {
            @Override
            public void onEvent(int event, String pathName) {
                if (pathName == null) return;
                handleEvent(event, path + "/" + pathName);
            }
        };
        observer.startWatching();
        observers.put(path, observer);
        
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".")) {
                    startObserving(child.getAbsolutePath());
                }
            }
        }
    }

    private void handleEvent(int event, String fullPath) {
        int e = event & FileObserver.ALL_EVENTS;
        File file = new File(fullPath);
        
        if (e == FileObserver.CREATE || e == FileObserver.MOVED_TO) {
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                startObserving(fullPath);
            }
            sendDelta(fullPath, "UPSERT");
        } else if (e == FileObserver.DELETE || e == FileObserver.MOVED_FROM) {
            if (observers.containsKey(fullPath)) {
                observers.get(fullPath).stopWatching();
                observers.remove(fullPath);
            }
            sendDelta(fullPath, "DELETE");
        } else if (e == FileObserver.MODIFY) {
            sendDelta(fullPath, "UPSERT");
        }
    }

    private void sendDelta(String fullPath, String action) {
        executor.submit(() -> {
            try {
                String deviceId = DeviceIdentityManager.getDeviceID(context);
                if (deviceId == null) {
                    Log.w(TAG, "Device ID not found, skipping delta sync");
                    return;
                }

                File file = new File(fullPath);
                FileMetaDataDto dto = new FileMetaDataDto();
                dto.setPath(fullPath);
                dto.setDeviceID(deviceId);
                dto.setName(file.getName());

                String parentPath = file.getParent();
                dto.setParentPath(parentPath != null ? parentPath.replace("\\", "/") : "/");
                dto.setSize(file.length());
                dto.setLastModified(file.lastModified());
                dto.setIsDirectory(file.isDirectory());

                List<FileMetaDataDto> list = new ArrayList<>();
                list.add(dto);

                Log.d(TAG, "Sending delta: " + action + " for " + fullPath);

                Call<Void> call = action.equals("DELETE") ?
                        ApiClient.getApiService().syncDeltaDelete(deviceId, fullPath) :
                        ApiClient.getApiService().syncDeltaUpsert(deviceId, list);

                // Use execute() since we are already in a background thread from the executor
                Response<Void> response = call.execute();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Delta sync failed for " + fullPath + " HTTP " + response.code());
                } else {
                    Log.d(TAG, "Delta sync successful for " + fullPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Delta sync error for " + fullPath + ": " + e.getMessage());
                if (e instanceof java.net.ConnectException) {
                    Log.e(TAG, "Connection failed! Check if server is running at " + BuildConfig.BASE_URL);
                    Log.e(TAG, "If using Android Emulator (AVD), try changing BASE_URL to http://10.0.2.2:8354 in build.gradle");
                }
            }
        });
    }
}
