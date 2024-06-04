package com.example.testaudiorecord;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import com.naman14.androidlame.WaveReader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 100;
    private MediaPlayer mediaPlayer;
    private int PICKFILE_REQUEST_CODE = 123;
    BufferedOutputStream outputStream;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
    private static final int OUTPUT_STREAM_BUFFER = 8192;
    private Button btnStart,btnStop,btnPause,btnResume,btnConvert;
    WaveReader waveReader;
    Uri inputUri;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        btnStart=findViewById(R.id.btn_start);
        btnStop=findViewById(R.id.btn_stop);
        btnPause=findViewById(R.id.btn_pause);
        btnResume=findViewById(R.id.btn_resume);
        btnConvert=findViewById(R.id.btn_play_sound);


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

        btnConvert.setOnClickListener(v -> {
//            setupMediaPlayer();

            pickFile();
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

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("pcm/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String filename = data.getDataString();
                inputUri = data.getData();
                if (inputUri != null) {
//                    encode();
                    boolean result=isWavFile(inputUri);
                    Log.e("dfdf", "onActivityResult: "+result );
                }
            }
        }
    }

    private void encode() {

        File input = new File(getRealPathFromURI(inputUri));
        final File output = new File(Environment.getExternalStorageDirectory() + "/testencode.mp3");

        int CHUNK_SIZE = 8192;

        addLog("Initialising wav reader");
        waveReader = new WaveReader(input);

        try {
            waveReader.openWave();
        } catch (IOException e) {
            Log.e("dfdf", "encode: ",e );
            e.printStackTrace();
        }

        addLog("Intitialising encoder");
        AndroidLame androidLame = new LameBuilder()
                .setInSampleRate(waveReader.getSampleRate())
                .setOutChannels(waveReader.getChannels())
                .setOutBitrate(128)
                .setOutSampleRate(waveReader.getSampleRate())
                .setQuality(5)
                .build();

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(output), OUTPUT_STREAM_BUFFER);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int bytesRead = 0;

        short[] buffer_l = new short[CHUNK_SIZE];
        short[] buffer_r = new short[CHUNK_SIZE];
        byte[] mp3Buf = new byte[CHUNK_SIZE];

        int channels = waveReader.getChannels();

        addLog("started encoding");
        while (true) {
            try {
                if (channels == 2) {

                    bytesRead = waveReader.read(buffer_l, buffer_r, CHUNK_SIZE);
                    addLog("bytes read=" + bytesRead);

                    if (bytesRead > 0) {

                        int bytesEncoded = 0;
                        bytesEncoded = androidLame.encode(buffer_l, buffer_r, bytesRead, mp3Buf);
                        addLog("bytes encoded=" + bytesEncoded);

                        if (bytesEncoded > 0) {
                            try {
                                addLog("writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else break;
                } else {

                    bytesRead = waveReader.read(buffer_l, CHUNK_SIZE);
                    addLog("bytes read=" + bytesRead);

                    if (bytesRead > 0) {
                        int bytesEncoded = 0;

                        bytesEncoded = androidLame.encode(buffer_l, buffer_l, bytesRead, mp3Buf);
                        addLog("bytes encoded=" + bytesEncoded);

                        if (bytesEncoded > 0) {
                            try {
                                addLog("writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else break;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        addLog("flushing final mp3buffer");
        int outputMp3buf = androidLame.flush(mp3Buf);
        addLog("flushed " + outputMp3buf + " bytes");

        if (outputMp3buf > 0) {
            try {
                addLog("writing final mp3buffer to outputstream");
                outputStream.write(mp3Buf, 0, outputMp3buf);
                addLog("closing output stream");
                outputStream.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void addLog(final String log) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public boolean isWavFile(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream == null) {
                return false;
            }

            byte[] header = new byte[12];
            int bytesRead = inputStream.read(header, 0, 12);

            if (bytesRead < 12) {
                return false;
            }

            return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                    header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E';
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}