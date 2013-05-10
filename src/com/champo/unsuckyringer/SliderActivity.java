package com.champo.unsuckyringer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;

public class SliderActivity extends Activity {

	private AudioManager audioManager;
	
	private Slider notificationSlider;
	private Slider ringerSlider;
	private Slider smsSlider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ringer_widget);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
		int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
		
		final SharedPreferences preferences = getSharedPreferences("volumes", 0);

		notificationSlider = (Slider) findViewById(R.id.notification_slider);
		notificationSlider.setMaxValue(maxVolume);
		notificationSlider.setSelectedValue(preferences.getInt("notification", currentVolume));
		notificationSlider.setSliderChangeListener(new Slider.OnSliderChangeListener() {
			
			@Override
			public void onSliderValuesChanged(Slider slider, int value) {
				preferences.edit()
					.putInt("notification", value)
					.commit();
			}
		});
		
		ringerSlider = (Slider) findViewById(R.id.ringer_slider);
		ringerSlider.setMaxValue(maxVolume);
		ringerSlider.setSelectedValue(preferences.getInt("ringer", currentVolume));
		ringerSlider.setSliderChangeListener(new Slider.OnSliderChangeListener() {
			
			@Override
			public void onSliderValuesChanged(Slider slider, int value) {
				preferences.edit()
					.putInt("ringer", value)
					.commit();
			}
		});
		
		smsSlider = (Slider) findViewById(R.id.sms_slider);
		smsSlider.setMaxValue(maxVolume);
		smsSlider.setSelectedValue(preferences.getInt("sms", currentVolume));
		smsSlider.setSliderChangeListener(new Slider.OnSliderChangeListener() {
			
			@Override
			public void onSliderValuesChanged(Slider slider, int value) {
				preferences.edit()
					.putInt("sms", value)
					.commit();
			}
		});
	}
	
}
