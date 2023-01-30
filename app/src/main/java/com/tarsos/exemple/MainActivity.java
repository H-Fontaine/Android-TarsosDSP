package com.tarsos.exemple;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.BeatRootSpectralFluxOnsetDetector;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {
    static private final String TAG = "MainActivity";


    static private final int PLAY_MUSIC_CODE = 0;
    static private final int BEAT_DETECTION_CODE = 1;

    private Button playButton;
    private Button beatDetectionButton;
    private TextView display;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = findViewById(R.id.main_activity_play_button);
        beatDetectionButton = findViewById(R.id.maine_activity_beat_detection_button);
        display = findViewById(R.id.main_activity_load_display);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, PLAY_MUSIC_CODE);
            }
        });

        beatDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, BEAT_DETECTION_CODE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri selectedFileUri = data.getData();
            switch (requestCode) {
                case PLAY_MUSIC_CODE :
                    playMusic(selectedFileUri);
                    break;
                case BEAT_DETECTION_CODE :
                    beatDetection(selectedFileUri);
            }
        }
    }

    private void playMusic(Uri selectedFileUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioDispatcher adp;
                adp = AudioDispatcherFactory.fromPipe(MainActivity.this, selectedFileUri, 0, -1,44100,5000,2500);
                adp.addAudioProcessor(new AndroidAudioPlayer(adp.getFormat(),6202, AudioManager.STREAM_MUSIC));
                adp.run();
            }
        }).start();
    }

    private void beatDetection(Uri selectedFileUri) {
        final double[] last_time = {0.0};
        int fftsize = 512;

        ComplexOnsetDetector complexOnsetDetector = new ComplexOnsetDetector(fftsize, 1.5);
        complexOnsetDetector.setHandler(new OnsetHandler() {
            @Override
            public void handleOnset(double time, double salience) {
                Log.d(TAG, "Music bpm : " + (int)(60 / (time - last_time[0])));
                display.setText("Music bpm : " + (int)(60 / (time - last_time[0])));
                last_time[0] = time;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioDispatcher adp;
                adp = AudioDispatcherFactory.fromPipe(MainActivity.this, selectedFileUri, 0, -1, 44100, fftsize, fftsize / 2 );
                adp.addAudioProcessor(complexOnsetDetector);
                adp.run();
            }
        }).start();
    }
}