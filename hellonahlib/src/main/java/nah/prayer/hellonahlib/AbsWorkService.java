package nah.prayer.hellonahlib;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

public abstract class AbsWorkService extends Service {

    protected static final int HASH_CODE = 1;

    protected boolean mFirstStarted = true;

    public static void cancelJobAlarmSub() {
        if (!ResurrectionEnv.sInitialized) return;
        ResurrectionEnv.sApp.sendBroadcast(new Intent(WakeUpReceiver.ACTION_CANCEL_JOB_ALARM_SUB));
    }

    public abstract Boolean shouldStopService(Intent intent, int flags, int startId);
    public abstract void startWork(Intent intent, int flags, int startId);
    public abstract void stopWork(Intent intent, int flags, int startId);

    public abstract Boolean isWorkRunning(Intent intent, int flags, int startId);
    @Nullable public abstract IBinder onBind(Intent intent, Void alwaysNull);
    public abstract void onServiceKilled(Intent rootIntent);

    protected int onStart(Intent intent, int flags, int startId) {

        ResurrectionEnv.startServiceMayBind(WatchDogService.class);

        Boolean shouldStopService = shouldStopService(intent, flags, startId);
        if (shouldStopService != null) {
            if (shouldStopService) stopService(intent, flags, startId); else startService(intent, flags, startId);
        }

        if (mFirstStarted) {
            mFirstStarted = false;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                startForeground(HASH_CODE, new Notification());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                    ResurrectionEnv.startServiceSafely(new Intent(getApplication(), WorkNotificationService.class));
            }
            getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), WatchDogService.class.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

        return START_STICKY;
    }

    void startService(Intent intent, int flags, int startId) {
        Boolean shouldStopService = shouldStopService(intent, flags, startId);
        if (shouldStopService != null && shouldStopService) return;
        Boolean workRunning = isWorkRunning(intent, flags, startId);
        if (workRunning != null && workRunning) return;
        startWork(intent, flags, startId);
    }

    void stopService(Intent intent, int flags, int startId) {
        stopWork(intent, flags, startId);
        cancelJobAlarmSub();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return onStart(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        onStart(intent, 0, 0);
        return onBind(intent, null);
    }

    protected void onEnd(Intent rootIntent) {
        onServiceKilled(rootIntent);
        if (!ResurrectionEnv.sInitialized) return;
        ResurrectionEnv.startServiceMayBind(ResurrectionEnv.sServiceClass);
        ResurrectionEnv.startServiceMayBind(WatchDogService.class);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onEnd(rootIntent);
    }

    @Override
    public void onDestroy() {
        onEnd(null);
    }

    public static class WorkNotificationService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(AbsWorkService.HASH_CODE, new Notification());
            stopSelf();
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
