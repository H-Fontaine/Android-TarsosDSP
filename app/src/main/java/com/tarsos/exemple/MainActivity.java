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
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;

public class MainActivity extends AppCompatActivity {
    static private final String TAG = "MainActivity";


    static private final int SELECTED_DOCUMENT_CODE = 0;

    private Button loadFileButton;
    private TextView display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadFileButton = findViewById(R.id.main_activity_load_file_button);
        display = findViewById(R.id.main_activity_load_display);

        loadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, SELECTED_DOCUMENT_CODE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECTED_DOCUMENT_CODE && resultCode == RESULT_OK) {
            Uri selectedFileUri = data.getData();
            playMusic(selectedFileUri);
        }
    }

    private void playMusic(Uri selectedFileUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioDispatcher adp;
                adp = AudioDispatcherFactory.fromPipe(MainActivity.this, selectedFileUri,44100,5000,2500);
                //adp = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
                adp.addAudioProcessor(new AndroidAudioPlayer(adp.getFormat(),6202, AudioManager.STREAM_MUSIC));
                adp.run();
            }
        }).start();
    }

}