package nah.prayer.hellonahlib;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import java.util.concurrent.TimeUnit;

public class WatchDogService extends Service {

    protected static final int HASH_CODE = 2;

    protected static Disposable sDisposable;
    protected static PendingIntent sPendingIntent;

    protected final int onStart(Intent intent, int flags, int startId) {

        if (!ResurrectionEnv.sInitialized) return START_STICKY;

        if (sDisposable != null && !sDisposable.isDisposed()) return START_STICKY;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            startForeground(HASH_CODE, new Notification());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                ResurrectionEnv.startServiceSafely(new Intent(ResurrectionEnv.sApp, WatchDogNotificationService.class));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobInfo.Builder builder = new JobInfo.Builder(HASH_CODE, new ComponentName(ResurrectionEnv.sApp, JobSchedulerService.class));
            builder.setPeriodic(ResurrectionEnv.getWakeUpInterval());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
            builder.setPersisted(true);
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        } else {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(ResurrectionEnv.sApp, ResurrectionEnv.sServiceClass);
            sPendingIntent = PendingIntent.getService(ResurrectionEnv.sApp, HASH_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ResurrectionEnv.getWakeUpInterval(), ResurrectionEnv.getWakeUpInterval(), sPendingIntent);
        }

        sDisposable = Observable
                .interval(ResurrectionEnv.getWakeUpInterval(), TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        ResurrectionEnv.startServiceMayBind(ResurrectionEnv.sServiceClass);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });

        getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), ResurrectionEnv.sServiceClass.getName()),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        return START_STICKY;
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        return onStart(intent, flags, startId);
    }

    @Override
    public final IBinder onBind(Intent intent) {
        onStart(intent, 0, 0);
        return null;
    }

    protected void onEnd(Intent rootIntent) {
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

    public static void cancelJobAlarmSub() {
        if (!ResurrectionEnv.sInitialized) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) ResurrectionEnv.sApp.getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.cancel(HASH_CODE);
        } else {
            AlarmManager am = (AlarmManager) ResurrectionEnv.sApp.getSystemService(ALARM_SERVICE);
            if (sPendingIntent != null) am.cancel(sPendingIntent);
        }
        if (sDisposable != null) sDisposable.dispose();
    }

    public static class WatchDogNotificationService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(WatchDogService.HASH_CODE, new Notification());
            stopSelf();
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
