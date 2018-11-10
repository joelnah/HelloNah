package nah.prayer.hellonah;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import nah.prayer.hellonahlib.AbsWorkService;
import nah.prayer.hellonahlib.ResurrectionEnv;

import java.util.concurrent.TimeUnit;

public class HelloNahService extends AbsWorkService {

    //Is the task completed and the service is no longer needed?
    public static boolean sShouldStopService;
    public static long sTimeSet = 0;
    public static Disposable sDisposable;

    private static TimeEventListener sTimeEventListener;
    public interface TimeEventListener{
        void onReceivedEvent(long count);
    }
    public static void setOnTimeEventListener(TimeEventListener listener){
        sTimeEventListener = listener;
    }

    public static void startService(Context context, long time) {
        if(!sShouldStopService)stopService();

        ResurrectionEnv.initialize(context, HelloNahService.class, ResurrectionEnv.DEFAULT_WAKE_UP_INTERVAL);
        sShouldStopService = false;
        sTimeSet = time;
        ResurrectionEnv.startServiceMayBind(HelloNahService.class);
    }
    public static void stopService() {
        //We no longer need the service to run now, set the flag to true
        sShouldStopService = true;
        //Unsubscribe from a task
        if (sDisposable != null) sDisposable.dispose();
        //cancel Job / Alarm / Subscription
        cancelJobAlarmSub();
    }



    /**
     * Is the task completed and the service is no longer needed?
     * @return Should stop the service, true; Should start the service, false; Unable to judge, do nothing, null.
     */
    @Override
    public Boolean shouldStopService(Intent intent, int flags, int startId) {
        return sShouldStopService;
    }

    @Override
    public void startWork(Intent intent, int flags, int startId) {
        Log.d("nah","Check if there is data saved on the disk when it was last destroyed");
        sDisposable = Observable
                .interval(sTimeSet, TimeUnit.SECONDS)
                //Cancel timed wakeup when canceling a task
                .doOnDispose(() -> {
                    Log.d("nah","Save data to disk");
                })
                .subscribe(count -> {
                    if(sTimeEventListener != null)
                        sTimeEventListener.onReceivedEvent(count);
                });
    }

    @Override
    public void stopWork(Intent intent, int flags, int startId) {
        stopService();
    }

    /**
     * Whether the task is running?
     * @return Task is running, true; The task is not currently running, false; Unable to judge, do nothing, null.
     */
    @Override
    public Boolean isWorkRunning(Intent intent, int flags, int startId) {
        //If you have not canceled your subscription, the task is still running.
        return sDisposable != null && !sDisposable.isDisposed();
    }

    @Override
    public IBinder onBind(Intent intent, Void v) {
        return null;
    }

    @Override
    public void onServiceKilled(Intent rootIntent) {
        Log.d("nah","Save data to disk");
    }
}
