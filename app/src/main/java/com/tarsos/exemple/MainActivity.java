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

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;

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
        int fftsize = 2048;

        OnsetHandler onsetHandler = new OnsetHandler() {
            private double last_time = 0;
            @Override
            public void handleOnset(double time, double salience) {
                Log.d(TAG, "Music bpm : " + (int)(60 / (time - last_time)) + " Salience : " + salience);
                display.setText("Music bpm : " + (int)(60 / (time - last_time)));
                last_time = time;
                //Log.d(TAG, "BPM ? : " + time);
            }
        };

        String inputFile = FFmpegKitConfig.getSafParameterForRead(MainActivity.this, selectedFileUri);
        String outputFile = AudioDispatcherFactory.registerNewPipe(MainActivity.this);
        //String outputFile = "meta.txt";
        FFmpegKit.execute("-y -loglevel error -i " + inputFile + " -f ffmetadata " + outputFile);
        File file = new File(outputFile);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("TBPM=")) {
                    String message = "Music BPM = " + line.substring(5);
                    Log.d(TAG, message);
                    display.setText(message);
                    return;
                }
            }
            br.close();
            AudioDispatcherFactory.closePipe(outputFile);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't open file");
            e.printStackTrace();
        }


        AudioDispatcher dispatcher;
        dispatcher = AudioDispatcherFactory.fromPipe(MainActivity.this, selectedFileUri, 0, -1, 44100, fftsize, fftsize / 2 );

        ComplexOnsetDetector detector = new ComplexOnsetDetector(fftsize);
        BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
        detector.setHandler(handler);

        dispatcher.addAudioProcessor(detector);
        dispatcher.run();

        handler.trackBeats(onsetHandler);
    }
}