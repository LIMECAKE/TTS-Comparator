package kr.devx.ttscomparator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "TTS-Comparator";

	private Button buttonPrevious, buttonNext;
	private EditText editor;
	private Button buttonGoogle, buttonNaver, buttonAmazon;

	private ArrayList<String> englishWords;
	private static int englishWordIndex = 0;

	// GOOGLE TTS
	private TextToSpeech googleTTS;
	private SeekBar googleSeekerPitch, googleSeekerSpeed;
	private Spinner googleSpinner;
	private ArrayList<Voice> googleVoices;
	private ArrayAdapter<Voice> googleAdapter;

	// NAVER CLOVA
	private CssProc.NaverTTSTask naverTTS;
	private SeekBar naverSeekerSpeed;
	private Spinner naverSpinner;
	private ArrayList<String> naverVoices;
	private ArrayAdapter<String> naverAdapter;
	private String naverVoice;
	private int naverSpeed = 0;

	// AMAZON POLLY
	private AmazonPollyPresigningClient amazonTTS;
	private Spinner amazonSpinner;
	private List<com.amazonaws.services.polly.model.Voice> amazonVoices;
	private ArrayAdapter<com.amazonaws.services.polly.model.Voice> amazonAdapter;
	private com.amazonaws.services.polly.model.Voice amazonVoice;
	private MediaPlayer amazonPlayer;

	private static final int REQUEST_PERMISSION_STORAGE = 1111;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initializeResources();
		initializeListeners();
		initializeEngines();

		editor.setText(englishWords.get(englishWordIndex));

		boolean permissionStorageGranted = ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
		if (!permissionStorageGranted) ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
	}

	private void initializeResources() {
		buttonPrevious = findViewById(R.id.button_previous);
		buttonNext = findViewById(R.id.button_next);
		editor = findViewById(R.id.editor);
		buttonGoogle = findViewById(R.id.button_google);
		buttonNaver = findViewById(R.id.button_naver);
		buttonAmazon = findViewById(R.id.button_amazon);
		googleSeekerPitch = findViewById(R.id.seekbar_google_pitch);
		googleSeekerSpeed = findViewById(R.id.seekbar_google_speed);
		googleSpinner = findViewById(R.id.spinner_google_voice);
		naverSeekerSpeed = findViewById(R.id.seekbar_naver_speed);
		naverSpinner = findViewById(R.id.spinner_naver_voice);
		amazonSpinner = findViewById(R.id.spinner_amazon_voice);

		englishWords = new ArrayList<>();
		initializeWords(englishWords);
	}

	private void initializeListeners() {
		buttonPrevious.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				englishWordIndex --;
				if (englishWordIndex < 0) englishWordIndex = englishWords.size() - 1;
				editor.setText(englishWords.get(englishWordIndex));
			}
		});
		buttonNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				englishWordIndex ++;
				if (englishWordIndex == englishWords.size()) englishWordIndex = 0;
				editor.setText(englishWords.get(englishWordIndex));
			}
		});
		googleSeekerPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress == 0) googleTTS.setPitch(0.5f);
				if (progress == 1) googleTTS.setPitch(1.0f);
				if (progress == 2) googleTTS.setPitch(2.0f);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		googleSeekerSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (progress == 0) googleTTS.setSpeechRate(0.5f);
				if (progress == 1) googleTTS.setSpeechRate(1.0f);
				if (progress == 2) googleTTS.setSpeechRate(2.0f);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		googleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				googleTTS.setVoice(googleVoices.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		naverSeekerSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				naverSpeed = (progress - 5) * -1;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
		naverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				naverVoice = naverVoices.get(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		amazonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				amazonVoice = amazonVoices.get(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		buttonGoogle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				googleTTS.speak(editor.getText().toString().trim(), TextToSpeech.QUEUE_FLUSH, null, null);
			}
		});
		buttonNaver.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				naverTTS = new CssProc.NaverTTSTask(MainActivity.this);
				naverTTS.execute(editor.getText().toString().trim(), naverVoice, String.valueOf(naverSpeed), Constants.NAVER_CLIENT_ID, Constants.NAVER_CLIENT_SECRET);
			}
		});
		buttonAmazon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest = new SynthesizeSpeechPresignRequest()
								.withText(editor.getText().toString().trim())
								.withVoiceId(amazonVoice.getId())
								.withOutputFormat(OutputFormat.Mp3);
				URL presignedSynthesizeSpeechUrl = amazonTTS.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

				Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

				if (amazonPlayer == null || amazonPlayer.isPlaying()) {
					setupNewMediaPlayer();
				}

				amazonPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());

				try {
					amazonPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
				} catch (IOException e) {
					Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
				}

				amazonPlayer.prepareAsync();
			}
		});
	}

	private void initializeEngines() {
		// GOOGLE
		googleTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				googleTTS.setLanguage(Locale.ENGLISH);
				if (googleVoices != null) googleVoices.clear();
				googleVoices = new ArrayList<>();
				for (Voice voice : googleTTS.getVoices()) {
					if (voice.getLocale() == Locale.US) googleVoices.add(voice);
				}
				if (googleVoices.isEmpty()) {
					for (Voice voice : googleTTS.getVoices()) {
						if (voice.getLocale() == Locale.KOREA) googleVoices.add(voice);
					}
				}
				googleAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, googleVoices);
				googleSpinner.setAdapter(googleAdapter);
			}
		});

		// NAVER
		naverVoices = new ArrayList<>();
		naverVoices.add("clara");
		naverVoices.add("matt");
		naverVoice = naverVoices.get(0);
		naverAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, naverVoices);
		naverSpinner.setAdapter(naverAdapter);

		// AMAZON
		CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(getApplicationContext(), Constants.AMAZON_POOL_ID, Regions.AP_NORTHEAST_2);
		amazonTTS = new AmazonPollyPresigningClient(credentialsProvider);

		Thread voiceGettingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (amazonVoices == null) {
					DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
					try {
						amazonVoices = new ArrayList<>();
						DescribeVoicesResult describeVoicesResult = amazonTTS.describeVoices(describeVoicesRequest);
						for (com.amazonaws.services.polly.model.Voice voice : describeVoicesResult.getVoices()) {
							if (voice.getLanguageCode().contains("US")) amazonVoices.add(voice);
							if (voice.getLanguageCode().contains("KR")) amazonVoices.add(voice);
						}
						Log.i(TAG, "Available Polly voices: " + amazonVoices);

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								amazonAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, amazonVoices);
								amazonSpinner.setAdapter(amazonAdapter);
								amazonVoice = amazonVoices.get(0);
								buttonAmazon.setEnabled(true);
								amazonSpinner.setEnabled(true);
							}
						});

					} catch (RuntimeException e) {
						Log.e(TAG, "Unable to get available voices.", e);
						return;
					}
				}
			}
		});
		voiceGettingThread.start();
		buttonAmazon.setText("LOADING");
		buttonAmazon.setEnabled(false);
		amazonSpinner.setEnabled(false);
	}

	private void initializeWords(ArrayList<String> words) {
		words.add("economic");
		words.add("growth");
		words.add("reparation");
		words.add("positive");
		words.add("management");
		words.add("thread");
		words.add("suppose");
		words.add("patience");
		words.add("generation");
		words.add("ordinary");
		words.add("creative");
		words.add("compensation");
		words.add("negative");
		words.add("tip");
		words.add("needle");
		words.add("fight");
		words.add("possess");
		words.add("gap");
		words.add("grace");
		words.add("imitation");
		words.add("come");
		words.add("income");
		words.add("letter");
		words.add("hold");
		words.add("human");
		words.add("linguistic");
		words.add("country");
		words.add("hospital");
		words.add("tradition");
		words.add("go");
		words.add("outcome");
		words.add("character");
		words.add("water");
		words.add("right");
		words.add("competence");
		words.add("beautiful");
		words.add("prescription");
		words.add("instrument");
	}

	void setupNewMediaPlayer() {
		amazonPlayer = new MediaPlayer();
		amazonPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.release();
				setupNewMediaPlayer();
			}
		});
		amazonPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.start();
			}
		});
		amazonPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				return false;
			}
		});
	}

}
