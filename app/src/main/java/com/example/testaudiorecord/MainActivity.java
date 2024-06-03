package com.example.testaudiorecord;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 100;
    private MediaPlayer mediaPlayer;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};

    private Button btnStart,btnStop,btnPause,btnResume,btnPlayBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        btnStart=findViewById(R.id.btn_start);
        btnStop=findViewById(R.id.btn_stop);
        btnPause=findViewById(R.id.btn_pause);
        btnResume=findViewById(R.id.btn_resume);
        btnPlayBack=findViewById(R.id.btn_play_sound);


        btnStart.setOnClickListener(v -> {
            Intent intent=new Intent(this, MyService.class);
            intent.setAction("START");
            startService(intent);
        });

        btnStop.setOnClickListener(v -> {
            Intent intent=new Intent(this, MyService.class);
            intent.setAction("STOP");
            stopService(intent);
        });

        btnPause.setOnClickListener(v -> {
            Intent intent=new Intent(this, MyService.class);
            intent.setAction("PAUSE");
            stopService(intent);
        });

        btnResume.setOnClickListener(v -> {
            Intent intent=new Intent(this, MyService.class);
            intent.setAction("RESUME");
            stopService(intent);
        });

        btnPlayBack.setOnClickListener(v -> {
            setupMediaPlayer();
        });
    }
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}");
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } else {
                // Quyền đã được cấp, tiếp tục xử lý logic của bạn

                Log.e("dfdf", "requestPermission: granted" );
            }
        } else {
            // Đối với các phiên bản thấp hơn Android 11, yêu cầu quyền truy cập bộ nhớ bình thường
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_MANAGE_EXTERNAL_STORAGE);
        }
    }
    private void setupMediaPlayer() {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath()+"/test_music_haha/audiorecordtest123.mp3";

        File file=new File(filePath);

        if (file.exists()) {
            Log.e("dfdf", "setupMediaPlayer: file exist" );
            mediaPlayer = new MediaPlayer();
            try {


                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build());

               mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                   @Override
                   public void onPrepared(MediaPlayer mp) {
                       Log.e("dfdf", "onPrepared: vcdfhasdfh" );
                       mp.start();
                   }
               });
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e("dfdf", "setupMediaPlayer: catch",e );
                e.printStackTrace();
            }
        } else {
            Log.e("dfdf", "setupMediaPlayer: file not exist" );
        }
    }

}