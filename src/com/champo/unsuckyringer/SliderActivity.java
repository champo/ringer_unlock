package com.champo.unsuckyringer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;

public class SliderActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ringer_widget);
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
		int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

		final SharedPreferences preferences = getSharedPreferences("volumes", 0);

		configureSlider(R.id.notification_slider, VolumeType.NOTIFICATION, preferences, currentVolume, maxVolume);
		configureSlider(R.id.ringer_slider, VolumeType.RINGER, preferences, currentVolume, maxVolume);
		configureSlider(R.id.sms_slider, VolumeType.SMS, preferences, currentVolume, maxVolume);
	}

	private void configureSlider(
			final int viewId, final VolumeType type, final SharedPreferences preferences, final int currentVolume, final int maxVolume) {

		Slider slider = (Slider) findViewById(viewId);
		slider.setMaxValue(maxVolume);
		slider.setSliderChangeListener(new Slider.OnSliderChangeListener() {

			@Override
			public void onSliderValuesChanged(final Slider slider, final int value) {
				preferences.edit()
					.putInt(type.name(), value)
					.commit();
			}
		});

		// This has to go after setting the listener so that value in the preferences gets setup
		slider.setSelectedValue(preferences.getInt(type.name(), currentVolume));
	}

}
