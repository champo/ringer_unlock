package com.champo.unsuckyringer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class VolumeService extends Service {

	private SharedPreferences preferences;

	private AudioManager audioManager;

	private VolumeType currentType = VolumeType.NOTIFICATION;

	private final PhoneStateListener stateListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(final int state, final String incomingNumber) {

			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				currentType = VolumeType.RINGER;
				break;

			case TelephonyManager.CALL_STATE_IDLE:
			case TelephonyManager.CALL_STATE_OFFHOOK:
				currentType = VolumeType.NOTIFICATION;
				break;
			}

			updateVolume();
		};

	};

	private final OnSharedPreferenceChangeListener preferenceChangeLister = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
			if (currentType.name().equals(key)) {
				updateVolume();
			}
		}
	};

	@Override
	public void onCreate() {
		preferences = getSharedPreferences("volumes", 0);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		Log.d("VolumeService", "Hooking up to telephony events...");
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE);

		preferences.registerOnSharedPreferenceChangeListener(preferenceChangeLister);
	}

	@Override
	public void onDestroy() {
		Log.d("VolumeService", "Unhooking telephony events");
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(stateListener, 0);

		preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeLister);

		preferences = null;
		audioManager = null;
	}

	private void updateVolume() {

		if (preferences == null || audioManager == null) {
			Log.d("VolumeService", "Got updateVolume call without preferences or audioManager");
			return;
		}

		if (preferences.contains(currentType.name())) {
			int volume = preferences.getInt(currentType.name(), 0);
			Log.d("VolumeService", "Updating volume to " + volume + " for type " + currentType);
			audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
		} else {
			Log.d("VolumeService", "Ignoring volume update for type = " + currentType);
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}
}
