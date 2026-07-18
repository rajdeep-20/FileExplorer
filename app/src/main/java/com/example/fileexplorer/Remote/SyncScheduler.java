package com.example.fileexplorer.Remote;


import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SyncScheduler {

    private final String TAG = "RFE.Scheduler";

    private final String WORK_PERIODIC_SYNC = "ref_periodic_metadata_sync";

    private final String WORK_PERIODIC_JOBS = "ref_periodic_jobs_check";

    private final String WORK_IMMEDIATE_SYNC = "rfe_immediate_sync";
    private final String WORK_IMMEDIATE_JOBS = "ref_immediate_job_check";




    private final Long PERIODIC_INTERVAL_TIME = 1L;

    public void schedulePeriodicWork(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        Constraints constraints = buildNetworkConstraint();

        // Periodic metadata sync (every 6 hours)
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                MetadataSyncWorker.class, PERIODIC_INTERVAL_TIME, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("rfe_sync")
                .build();

        wm.enqueueUniquePeriodicWork(WORK_PERIODIC_SYNC, ExistingPeriodicWorkPolicy.KEEP, syncRequest);

        // Periodic job check (every 6 hours — safety net for missed FCM)
        PeriodicWorkRequest jobRequest = new PeriodicWorkRequest.Builder(
                JobProcessorWorker.class, PERIODIC_INTERVAL_TIME, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("rfe_jobs")
                .build();

        wm.enqueueUniquePeriodicWork(WORK_PERIODIC_JOBS, ExistingPeriodicWorkPolicy.KEEP, jobRequest);

        Log.i(TAG, "Periodic work scheduled (every " + PERIODIC_INTERVAL_TIME + "h).");
    }

    public void triggerImmediateSync(Context context){
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MetadataSyncWorker.class)
                .setConstraints(buildNetworkConstraint())
                .addTag("ref_sync_immediate")
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_IMMEDIATE_SYNC, ExistingWorkPolicy.REPLACE, request);
        Log.i(TAG,  "Immediate meta data sync enqueued");
    }
    public void triggerImmediateJobCheck(Context context){
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(JobProcessorWorker.class)
                .setConstraints(buildNetworkConstraint())
                .addTag("ref_jobs_immediate")
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_IMMEDIATE_JOBS, ExistingWorkPolicy.REPLACE, request);
        Log.i(TAG,  "Immediate job check enqueued");
    }
    private Constraints buildNetworkConstraint(){
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }


}
