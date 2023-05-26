package org.evolution.pixelparts.services;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.preference.PreferenceManager;

import org.evolution.pixelparts.misc.Constants;
import org.evolution.pixelparts.utils.Utils;

public class AutoHBMService extends Service {

    private static boolean mAutoHBMActive = false;

    private SensorManager mSensorManager;
    Sensor mLightSensor;

    private SharedPreferences mSharedPrefs;

    public void activateLightSensorRead() {
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void deactivateLightSensorRead() {
        mSensorManager.unregisterListener(mSensorEventListener);
        mAutoHBMActive = false;
        enableHBM(false);
    }

    private void enableHBM(boolean enable) {
        if (enable) {
            Utils.writeValue(Constants.NODE_HBM, "1");
        } else {
            Utils.writeValue(Constants.NODE_HBM, "0");
        }
    }

    private boolean isCurrentlyEnabled() {
        String fileValue = Utils.getFileValue(Constants.NODE_HBM, "0");
        return fileValue.equals("1") ? true : false;
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {
        private long mLastTriggerTime = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            KeyguardManager km =
                    (KeyguardManager) getSystemService(getApplicationContext().KEYGUARD_SERVICE);
            boolean keyguardShowing = km.inKeyguardRestrictedInputMode();
            float luxThreshold = Float.parseFloat(mSharedPrefs.getString(Constants.KEY_AUTO_HBM_THRESHOLD, "20000"));
            long timeToDisableHBM = Long.parseLong(mSharedPrefs.getString(Constants.KEY_HBM_DISABLE_TIME, "1"));

            if (lux > luxThreshold) {
                if ((!mAutoHBMActive || !isCurrentlyEnabled()) && !keyguardShowing) {
                    mAutoHBMActive = true;
                    enableHBM(true);
                    mLastTriggerTime = System.currentTimeMillis();
                }
            } else {
                if (mAutoHBMActive) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mLastTriggerTime >= timeToDisableHBM * 1000) {
                        mAutoHBMActive = false;
                        enableHBM(false);
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                activateLightSensorRead();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                deactivateLightSensorRead();
            }
        }
    };

    @Override
    public void onCreate() {
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            deactivateLightSensorRead();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
