package com.champo.unsuckyringer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SliderActivity extends Activity {

	private static final String LINKED = "linked";

	private CheckBox linkVolumes;
	private Slider notificationSlider;
	private Slider ringerSlider;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ringer_widget);
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
		int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

		final SharedPreferences preferences = getSharedPreferences("volumes", 0);

		linkVolumes = (CheckBox) findViewById(R.id.link_volume);
		linkVolumes.setChecked(preferences.getBoolean(LINKED, false));

		notificationSlider = configureSlider(R.id.notification_slider, VolumeType.NOTIFICATION, preferences, currentVolume, maxVolume);
		ringerSlider = configureSlider(R.id.ringer_slider, VolumeType.RINGER, preferences, currentVolume, maxVolume);

		linkVolumes.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {

				if (isChecked) {
					notificationSlider.setSelectedValue(ringerSlider.getSelectedValue());
				}

				preferences.edit()
					.putBoolean(LINKED, isChecked)
					.commit();
			}
		});

		configureButton(R.id.set_full, maxVolume, maxVolume);
		configureButton(R.id.set_silent, 0, 0);
		configureButton(R.id.set_sleep, maxVolume, 0);
	}

	private void configureButton(final int viewId, final int ringerVolume, final int notificationVolume) {

		Button button = (Button) findViewById(viewId);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				linkVolumes.setChecked(false);
				ringerSlider.setSelectedValue(ringerVolume);
				notificationSlider.setSelectedValue(notificationVolume);
			}
		});
	}

	private Slider configureSlider(
			final int viewId, final VolumeType type, final SharedPreferences preferences, final int currentVolume, final int maxVolume) {

		Slider slider = (Slider) findViewById(viewId);
		slider.setMaxValue(maxVolume);
		slider.setNotifyWhileDragging(true);
		slider.setSliderChangeListener(new Slider.OnSliderChangeListener() {

			@Override
			public void onSliderValuesChanged(final Slider slider, final int value) {
				preferences.edit()
					.putInt(type.name(), value)
					.commit();

				if (linkVolumes.isChecked()) {

					Slider other = ringerSlider;
					if (slider == ringerSlider) {
						other = notificationSlider;
					}

					if (other != null && other.getSelectedValue() != value) {
						other.setSelectedValue(value);
					}
				}
			}
		});

		// This has to go after setting the listener so that value in the preferences gets setup
		slider.setSelectedValue(preferences.getInt(type.name(), currentVolume));

		return slider;
	}

}
