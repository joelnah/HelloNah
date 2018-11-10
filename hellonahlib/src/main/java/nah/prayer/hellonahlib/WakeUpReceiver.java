package nah.prayer.hellonahlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WakeUpReceiver extends BroadcastReceiver {

    protected static final String ACTION_CANCEL_JOB_ALARM_SUB = "nah.prayer.hellonahlib.CANCEL_JOB_ALARM_SUB";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_CANCEL_JOB_ALARM_SUB.equals(intent.getAction())) {
            WatchDogService.cancelJobAlarmSub();
            return;
        }
        if (!ResurrectionEnv.sInitialized) return;
        ResurrectionEnv.startServiceMayBind(ResurrectionEnv.sServiceClass);
    }

    public static class WakeUpAutoStartReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ResurrectionEnv.sInitialized) return;
            ResurrectionEnv.startServiceMayBind(ResurrectionEnv.sServiceClass);
        }
    }
}
