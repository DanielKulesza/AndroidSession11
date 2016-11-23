package scs2682.audio;

import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class AppActivity extends AppCompatActivity {
	private static final int IDLE = -1;
	private static final int PREPARING =  0;
	private static final int PLAYING = 1;
	private static final int PAUSE = 2;

	private int mediaPlayerState = IDLE;

	// arrays of MP3 files to play
	private String[] mp3Titles;
	private String[] mp3Urls;

	// define drawables for play and pause
	private Drawable playDrawable;
	private Drawable pauseDrawable;

	private View player;

	private TextView time;
	private TextView totalTime;
	private ImageView playPause;
	private SeekBar seek;

	private final MediaPlayer mediaPlayer = new MediaPlayer();
	//handler is needed for communication between threads
	private final Handler handler = new Handler(Looper.getMainLooper());

	private final Runnable timeRunnable = new Runnable() {
		@Override
		public void run() {
			if(AppActivity.this.isFinishing() || AppActivity.this.isDestroyed()){
				//activity is down, just return
				return;
			}
			int currentPosition = mediaPlayer.getCurrentPosition();
			time.setText(fromSecondsToPlaybackTime(currentPosition/1000L, ":"));
			seek.setProgress(currentPosition);

			handler.postDelayed(this, 1000L);

		}
	};

	private final MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mediaPlayer) {
			mediaPlayer.start();
			player.setVisibility(View.VISIBLE);
			mediaPlayerState = PLAYING;

			int duration = mediaPlayer.getDuration() > -1 ? mediaPlayer.getDuration() : 0;
			seek.setMax(duration);
			totalTime.setText(fromSecondsToPlaybackTime(duration / 1000L, ":"));
			playPause.setImageDrawable(pauseDrawable);

			handler.postDelayed(timeRunnable, 1000L);
		}
	};

	private final MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
			clearPlayer();
			Toast.makeText(AppActivity.this, "Error playing", Toast.LENGTH_SHORT).show();

			return true;
		}
	};

	private final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mediaPlayer) {
			clearPlayer();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appactivity);

		// get all view references
		final ViewGroup buttonContainer = (ViewGroup) findViewById(R.id.buttonContainer);
		time = (TextView) findViewById(R.id.time);
		totalTime = (TextView) findViewById(R.id.totalTime);

		playPause = (ImageView) findViewById(R.id.playPause);
		playPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(mediaPlayerState == PLAYING){
					//pause playback and switch the image to PLAY
					mediaPlayerState = PAUSE;
					mediaPlayer.pause();
					playPause.setImageDrawable(playDrawable);
				}
				else if (mediaPlayerState == PAUSE){
					mediaPlayerState = PLAYING;
					mediaPlayer.start();
					playPause.setImageDrawable(pauseDrawable);
				}
			}
		});

		seek = (SeekBar) findViewById(R.id.seek);
		seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int position, boolean isFromUser) {
				//NOTE: Always check isFromUser is true
				if (isFromUser){
					mediaPlayer.seekTo(position);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		final SeekBar volume = (SeekBar) findViewById(R.id.volume);
		volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
		volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int position, boolean isFromUser) {
				//Note : Always check isFromUser is true!
				if (isFromUser){
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,position,0);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		player = findViewById(R.id.player);

		mp3Titles = getResources().getStringArray(R.array.mp3Titles);
		mp3Urls = getResources().getStringArray(R.array.mp3Urls);

		playDrawable = getResources().getDrawable(android.R.drawable.ic_media_play);
		pauseDrawable = getResources().getDrawable(android.R.drawable.ic_media_pause);
		if (savedInstanceState == null) {
			//app was just started and not from saved instance state
			for (String mp3Title : mp3Titles) {
				//create a button in code (dynamically
				Button button = new Button(this);
				button.setText(mp3Title);
				button.setTag(buttonContainer.getChildCount());
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						int position = (int) view.getTag();
						play(position);
					}
				});
				buttonContainer.addView(button, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

			}
		}
		//initialize mediaPlayer
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		mediaPlayer.setOnErrorListener(onErrorListener);
		mediaPlayer.setOnCompletionListener(onCompletionListener);
	}


	@Override
	protected void onDestroy() {

		clearPlayer();
		mediaPlayer.release();
		mediaPlayer.setOnPreparedListener(null);
		mediaPlayer.setOnCompletionListener(null);
		mediaPlayer.setOnErrorListener(null);

		super.onDestroy();
	}

	/**
	 * COnvert seconds into visually enjoying string, e.g. 3:45, 12:06, 1:34:47, etc.
	 *
	 * @param timestamp timestamp in seconds
	 * @param delimiter delimeter between hours, minutes and seconds
	 * @return
	 */
	public static String fromSecondsToPlaybackTime(long timestamp, String delimiter) {
		long secondsInMinute = timestamp % 60;
		long minutesInHour = (timestamp / 60) % 60;
		long hoursInDay = (timestamp / (60 * 60)) % 24;

		String hoursPattern = "%d" + delimiter + "%02d" + delimiter + "%02d";
		String minutesPattern = "%d" + delimiter + "%02d";

		return hoursInDay > 0 ? String.format(hoursPattern, hoursInDay, minutesInHour, secondsInMinute)
			: String.format(minutesPattern, minutesInHour, secondsInMinute);
	}

	private void clearPlayer() {
		if (mediaPlayerState != IDLE){
			mediaPlayerState = IDLE;
			mediaPlayer.reset();

			player.setVisibility(View.GONE);
			time.setText("0:00");
			totalTime.setText("0:00");
			playPause.setImageDrawable(playDrawable);

			handler.removeCallbacksAndMessages(true);
		}
	}

	private void play(int index) {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
		String mp3Url = index > -1 && index < mp3Urls.length ? mp3Urls[index] : "";

		clearPlayer();

		if (TextUtils.isEmpty(mp3Url)) {
			Toast.makeText(this, "url is not valid", Toast.LENGTH_SHORT).show();
			return;
		}

		if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
			mediaPlayerState = PREPARING;

			try{
				mediaPlayer.setDataSource(mp3Url);
				mediaPlayer.prepareAsync();
			}
			catch(IOException | IllegalStateException e){
				e.printStackTrace();
				Toast.makeText(this, "Exception trying to play", Toast.LENGTH_SHORT).show();
				clearPlayer();
			}

		}
		else {
			Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show();
		}
	}
}