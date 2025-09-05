/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts.chargecontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.evolution.pixelparts.Constants;
import org.evolution.pixelparts.CustomSeekBarPreference;
import org.evolution.pixelparts.R;
import org.evolution.pixelparts.utils.FileUtils;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;

public class ChargeControlFragment extends SettingsBasePreferenceFragment
        implements OnCheckedChangeListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = ChargeControlFragment.class.getSimpleName();

    // Charge control preference
    private MainSwitchPreference mChargeControlSwitch;

    // Stop/Start preferences
    private CustomSeekBarPreference mStopChargingPreference;
    private CustomSeekBarPreference mStartChargingPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(TAG, "onCreatePreferences - Initializing charge control preferences.");
        setPreferencesFromResource(R.xml.charge_control, rootKey);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Log.d(TAG, "Shared preferences obtained.");

        // Charge control preference
        mChargeControlSwitch = findPreference(Constants.KEY_CHARGE_CONTROL);
        boolean isChargeControlEnabled = sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, false);
        mChargeControlSwitch.setChecked(isChargeControlEnabled);
        mChargeControlSwitch.addOnSwitchChangeListener(this);
        Log.d(TAG, "Charge control switch initialized: " + isChargeControlEnabled);

        // Stop preference
        mStopChargingPreference = findPreference(Constants.KEY_STOP_CHARGING);
        if (FileUtils.isFileWritable(Constants.NODE_STOP_CHARGING)) {
            int stopChargingValue = sharedPrefs.getInt(Constants.KEY_STOP_CHARGING,
                    Integer.parseInt(FileUtils.getFileValue(Constants.NODE_STOP_CHARGING, Constants.DEFAULT_STOP_CHARGING)));
            mStopChargingPreference.setValue(stopChargingValue);
            mStopChargingPreference.setOnPreferenceChangeListener(this);
            Log.d(TAG, "Stop charging preference initialized: " + stopChargingValue);
        } else {
            mStopChargingPreference.setSummary(getString(R.string.kernel_node_access_error));
            mStopChargingPreference.setEnabled(false);
            Log.d(TAG, "Stop charging preference disabled due to kernel node access error.");
        }
        mStopChargingPreference.setVisible(isChargeControlEnabled);

        // Start preference
        mStartChargingPreference = findPreference(Constants.KEY_START_CHARGING);
        if (FileUtils.isFileWritable(Constants.NODE_START_CHARGING)) {
            int startChargingValue = sharedPrefs.getInt(Constants.KEY_START_CHARGING,
                    Integer.parseInt(FileUtils.getFileValue(Constants.NODE_START_CHARGING, Constants.DEFAULT_START_CHARGING)));
            mStartChargingPreference.setValue(startChargingValue);
            mStartChargingPreference.setOnPreferenceChangeListener(this);
            Log.d(TAG, "Start charging preference initialized: " + startChargingValue);
        } else {
            mStartChargingPreference.setSummary(getString(R.string.kernel_node_access_error));
            mStartChargingPreference.setEnabled(false);
            Log.d(TAG, "Start charging preference disabled due to kernel node access error.");
        }
        mStartChargingPreference.setVisible(isChargeControlEnabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();

        prefChange.putBoolean(Constants.KEY_CHARGE_CONTROL, isChecked).apply();

        mStopChargingPreference.setVisible(isChecked);
        mStartChargingPreference.setVisible(isChecked);

        Log.d(TAG, "Charge control switch changed: " + isChecked);

        if (!isChecked) {
            // Stop preference
            int defaultStopCharging = 100;
            prefChange.putInt(Constants.KEY_STOP_CHARGING, defaultStopCharging).apply();
            FileUtils.writeValue(Constants.NODE_STOP_CHARGING, Integer.toString(defaultStopCharging));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mStopChargingPreference.refresh(defaultStopCharging);
            }, 750);

            // Start preference
            int defaultStartCharging = 0;
            prefChange.putInt(Constants.KEY_START_CHARGING, defaultStartCharging).apply();
            FileUtils.writeValue(Constants.NODE_START_CHARGING, Integer.toString(defaultStartCharging));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                mStartChargingPreference.refresh(defaultStartCharging);
            }, 750);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "Preference changed: " + preference.getKey() + " New value: " + newValue);

        // Stop preference
        if (preference == mStopChargingPreference) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int value = Integer.parseInt(newValue.toString());
            int stopLevel = Integer.parseInt(newValue.toString());
            int startLevel = sharedPrefs.getInt(Constants.KEY_START_CHARGING, 0);
            Log.d(TAG, "value is: " + value + " stopLevel is: " + stopLevel + " startLevel is: " + startLevel);
            if (startLevel >= stopLevel) {
                startLevel = stopLevel - 1;
                sharedPrefs.edit().putInt(Constants.KEY_START_CHARGING, startLevel).apply();
                FileUtils.writeValue(Constants.NODE_START_CHARGING, String.valueOf(startLevel));
                mStartChargingPreference.refresh(startLevel);
                Toast.makeText(getContext(), R.string.stop_below_start_error, Toast.LENGTH_SHORT).show();

            }
            Log.d(TAG, "editing sharedPrefs putting int value: " + value + " with key: " + Constants.KEY_STOP_CHARGING + " and applying it.");
            sharedPrefs.edit().putInt(Constants.KEY_STOP_CHARGING, value).apply();
            Log.d(TAG, "Writing to " + Constants.NODE_STOP_CHARGING + " value..." + value);
            FileUtils.writeValue(Constants.NODE_STOP_CHARGING, String.valueOf(value));
            return true;
        }
        // Start preference
        else if (preference == mStartChargingPreference) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int value = Integer.parseInt(newValue.toString());
            int startLevel = Integer.parseInt(newValue.toString());
            int stopLevel = sharedPrefs.getInt(Constants.KEY_STOP_CHARGING, 100);
            Log.d(TAG, "value is: " + value + " stopLevel is: " + stopLevel + " startLevel is: " + startLevel);
            if (stopLevel <= startLevel) {
                stopLevel = startLevel + 1;
                sharedPrefs.edit().putInt(Constants.KEY_STOP_CHARGING, stopLevel).apply();
                FileUtils.writeValue(Constants.NODE_STOP_CHARGING, String.valueOf(stopLevel));
                mStopChargingPreference.refresh(stopLevel);
                Toast.makeText(getContext(), R.string.start_above_stop_error, Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "editing sharedPrefs putting int value: " + value + " with key: " + Constants.KEY_START_CHARGING + " and applying it.");
            sharedPrefs.edit().putInt(Constants.KEY_START_CHARGING, value).apply();
            Log.d(TAG, "Writing to " + Constants.NODE_START_CHARGING + " value..." + value);
            FileUtils.writeValue(Constants.NODE_START_CHARGING, String.valueOf(value));
            return true;
        }

        return false;
    }

    // Stop preference
    public static void restoreStopChargingSetting(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean chargeControlEnabled = sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, true);

        Log.d(TAG, "Restoring stop charging setting...");

        if (chargeControlEnabled && FileUtils.isFileWritable(Constants.NODE_STOP_CHARGING)) {
            Log.d(TAG, "ChargeControlEnabled and File is writeable...");
            int value = sharedPrefs.getInt(Constants.KEY_STOP_CHARGING,
                    Integer.parseInt(FileUtils.getFileValue(Constants.NODE_STOP_CHARGING, Constants.DEFAULT_STOP_CHARGING)));
            Log.d(TAG, "Writing to " + Constants.NODE_STOP_CHARGING + " this value..." + value);
            FileUtils.writeValue(Constants.NODE_STOP_CHARGING, String.valueOf(value));
        }
    }

    // Start preference
    public static void restoreStartChargingSetting(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean chargeControlEnabled = sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, true);

        Log.d(TAG, "Restoring start charging setting...");

        if (chargeControlEnabled && FileUtils.isFileWritable(Constants.NODE_START_CHARGING)) {
            Log.d(TAG, "ChargeControlEnabled and File is writeable...");
            int value = sharedPrefs.getInt(Constants.KEY_START_CHARGING,
                    Integer.parseInt(FileUtils.getFileValue(Constants.NODE_START_CHARGING, Constants.DEFAULT_START_CHARGING)));
            Log.d(TAG, "Writing to " + Constants.NODE_START_CHARGING + " this value..." + value);
            FileUtils.writeValue(Constants.NODE_START_CHARGING, String.valueOf(value));
        }
    }
}
