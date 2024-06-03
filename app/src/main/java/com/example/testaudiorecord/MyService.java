package com.example.testaudiorecord;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyService extends Service {

    public static final String CHANNEL_ID = "record_channel";
    private static CharSequence CHANNEL_NAME = "record_channel_name";
    public static final int NOTIFICATION_ID = 1;

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private AudioRecord recorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private File audioFile;
    private Thread recordingThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case "START":

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startFgs(NOTIFICATION_ID, sendNotificationRecording());
                    } else {
                        startFgs(NOTIFICATION_ID, new Notification());
                    }
                    startRecording();
                    break;
                case "PAUSE":
                    pauseRecording();
                    break;
                case "RESUME":
                    resumeRecording();
                    break;
                case "STOP":
                    stopRecording();
                    break;
            }
        }


        return START_STICKY;
    }

    private void startFgs(int notificationId, Notification notificaton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notificaton, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(notificationId, notificaton);
        }
    }

    public Notification sendNotificationRecording() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSound(null)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("huhuhuhu")
                .setContentText("asdfasdfasdf")
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManagerCompat.notify(NOTIFICATION_ID, notification);
        }
        return notification;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void startRecording() {
        if (recorder == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);


            File musicDir= null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            }
            File audioDir=new File(musicDir,"test_music_haha");

            if (!audioDir.exists()){
                boolean result=audioDir.mkdirs();
                Log.e("dfdf", "startRecording: mkdirs "+result );
            }


            audioFile = new File(audioDir, "audiorecordtest123.mp3");

            recorder.startRecording();
            isRecording = true;
            isPaused = false;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }
    }

    private void writeAudioDataToFile() {
        byte[] data = new byte[BUFFER_SIZE];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioFile);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("dfdf", "writeAudioDataToFile: error file ouput stream" );
        }

        while (isRecording) {
            if (!isPaused) {
                int read = recorder.read(data, 0, BUFFER_SIZE);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(data, 0, read);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("dfdf", "writeAudioDataToFile: error write fails" );
                    }
                }
            }
        }

        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseRecording() {
        isPaused = true;
    }

    private void resumeRecording() {
        isPaused = false;
    }

    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }
}
