package com.example.testaudiorecord.test2;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.testaudiorecord.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class Service3 extends Service {

    String pathPlayBack = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioPlayback.pcm";
    String pathMic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioMic.pcm";
    String pathMerge = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioMerge.pcm";

    public static final String CHANNEL_ID = "record_channel";
    private static CharSequence CHANNEL_NAME = "record_channel_name";
    public static final int NOTIFICATION_ID = 1;

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private AudioRecord recorderPlayback, recordMic;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private File audioFilePlayback, audioFileMic;

    private Thread recordingThread;

    private MediaProjection mediaProjection;
    AudioPlaybackCaptureConfiguration config;
    private int mResultCode;
    private Intent mResultData;
    private AudioManager audioManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startFgs(NOTIFICATION_ID, sendNotificationRecording());
        } else {
            startFgs(NOTIFICATION_ID, new Notification());
        }

        if (action != null) {
            switch (action) {
                case "START":
                    mResultCode = intent.getIntExtra("data", -1);
                    mResultData = intent.getParcelableExtra("data");

                    initMediaProjection();

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
                case "PLAY_AUDIO":
                    playAudio();
                    break;
                case "MERGE":
                    mergeAudio(1.0f,0.1f);
                    break;
            }
        }

        return START_STICKY;
    }

    private void playAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE,
                AudioTrack.MODE_STREAM
        );

        audioTrack.play();

        try (FileInputStream fis = new FileInputStream(pathMerge)) {
            int read;
            while ((read = fis.read(buffer)) > 0) {
                audioTrack.write(buffer, 0, read);
            }
        } catch (IOException e) {

            e.printStackTrace();
        } finally {
            audioTrack.stop();
            audioTrack.release();
        }
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
        if (recorderPlayback == null) {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

                recorderPlayback = new AudioRecord.Builder()
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                .build())
                        .setAudioPlaybackCaptureConfig(config)
                        .setBufferSizeInBytes(BUFFER_SIZE)
                        .build();


                recordMic = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, 512);


            }


            File musicDir = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            }

            File audioDir = new File(musicDir, "test_music_haha");

            if (!audioDir.exists()) {
                boolean result = audioDir.mkdirs();
                Log.e("dfdf", "startRecording: mkdirs " + result);
            }


            audioFilePlayback = new File(pathPlayBack);
            audioFileMic = new File(pathMic);

            recordMic.startRecording();
            recorderPlayback.startRecording();
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
        byte[] dataPlayback = new byte[BUFFER_SIZE];
        FileOutputStream osPlayback = null;

        byte[] dataMic = new byte[BUFFER_SIZE];
        FileOutputStream osMic = null;

        try {
            osPlayback = new FileOutputStream(audioFilePlayback);
            osMic = new FileOutputStream(audioFileMic);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("dfdf", "writeAudioDataToFile: error file ouput stream");
        }

        while (isRecording) {
            if (!isPaused) {
                int readPlayback = recorderPlayback.read(dataPlayback, 0, BUFFER_SIZE);
                int readMic = recordMic.read(dataMic, 0, BUFFER_SIZE);

                if (readPlayback != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        osPlayback.write(dataPlayback, 0, readPlayback);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("dfdf", "writeAudioDataToFile: error write fails");
                    }
                }

                if (readMic != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        osMic.write(dataMic, 0, readMic);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("dfdf", "writeAudioDataToFile: error write fails");
                    }
                }
            }
        }

        try {
            if (osPlayback != null) {
                osPlayback.close();
                osMic.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaMuxer
    }

    private void stopRecording() {
        if (recorderPlayback != null) {
            isRecording = false;
            recorderPlayback.stop();
            recorderPlayback.release();
            recorderPlayback = null;
            isRecording = false;
            recordMic.stop();
            recordMic.release();
            recordMic = null;
            recordingThread = null;
        }

    }

    private void pauseRecording() {
        isPaused = true;
    }

    private void resumeRecording() {
        isPaused = false;
    }

    private void initMediaProjection() {
        mediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(mResultCode, mResultData);
        Handler handler = new Handler(Looper.getMainLooper());
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            // Nothing
            // We don't use it but register it to avoid runtime error from SDK 34+.
        }, handler);
    }

//    public void mergeAudio()throws IOException{
//
//
//        FileInputStream micInputStream = new FileInputStream(pathMic);
//        FileInputStream playbackInputStream = new FileInputStream(pathPlayBack);
//        FileOutputStream mergedOutputStream = new FileOutputStream(pathMerge);
//
//        byte[] micBuffer = new byte[1024];
//        byte[] playbackBuffer = new byte[1024];
//        byte[] mergedBuffer = new byte[1024];
//
//        int bytesReadMic;
//        int bytesReadPlayback;
//
//        // Đọc từng mẫu âm thanh từ cả hai file và gộp lại
//        while ((bytesReadMic = micInputStream.read(micBuffer)) != -1 &&
//                (bytesReadPlayback = playbackInputStream.read(playbackBuffer)) != -1) {
//            for (int i = 0; i < bytesReadMic; i++) {
//                // Lấy giá trị trung bình của mẫu âm thanh từ hai file
//                mergedBuffer[i] = (byte) ((micBuffer[i] + playbackBuffer[i]) / 2);
//            }
//            // Ghi dữ liệu gộp vào file mới
//            mergedOutputStream.write(mergedBuffer, 0, bytesReadMic);
//        }
//
//        micInputStream.close();
//        playbackInputStream.close();
//        mergedOutputStream.close();
//    }

    private void mergeAudio(float micVolume, float playbackVolume) {
//        File file1 = new File(pathMic);
//        File file2 = new File(pathPlayBack);
//        File outputFile = new File(pathMerge);
//
//        try (FileInputStream fis1 = new FileInputStream(file1);
//             FileInputStream fis2 = new FileInputStream(file2);
//             FileOutputStream fos = new FileOutputStream(outputFile)) {
//
//            byte[] buffer1 = new byte[1024];
//            byte[] buffer2 = new byte[1024];
//            int read1, read2 = 0;
//
//            while ((read1 = fis1.read(buffer1)) > 0 && (read2 = fis2.read(buffer2)) > 0) {
//                byte[] mixedBuffer = new byte[read1];
//                for (int i = 0; i < read1; i += 2) {
//                    short sample1 = (short) ((buffer1[i] & 0xFF) | (buffer1[i + 1] << 8));
//                    short sample2 = (short) ((buffer2[i] & 0xFF) | (buffer2[i + 1] << 8));
//                    short mixedSample = (short) Math.min(Math.max(sample1 + sample2, Short.MIN_VALUE), Short.MAX_VALUE);
//                    mixedBuffer[i] = (byte) (mixedSample & 0xFF);
//                    mixedBuffer[i + 1] = (byte) ((mixedSample >> 8) & 0xFF);
//                }
//                fos.write(mixedBuffer, 0, read1);
//            }
//
//            // Nếu một trong hai tệp vẫn còn dữ liệu
//            if (read1 > 0) {
//                fos.write(buffer1, 0, read1);
//                while ((read1 = fis1.read(buffer1)) > 0) {
//                    fos.write(buffer1, 0, read1);
//                }
//            } else if (read2 > 0) {
//                fos.write(buffer2, 0, read2);
//                while ((read2 = fis2.read(buffer2)) > 0) {
//                    fos.write(buffer2, 0, read2);
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        File file1 = new File(pathMic);
        File file2 = new File(pathPlayBack);
        File outputFile = new File(pathMerge);

        try (FileInputStream fis1 = new FileInputStream(file1);
             FileInputStream fis2 = new FileInputStream(file2);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer1 = new byte[1024];
            byte[] buffer2 = new byte[1024];
            int read1, read2 = 0;

            while ((read1 = fis1.read(buffer1)) > 0 && (read2 = fis2.read(buffer2)) > 0) {
                byte[] mixedBuffer = new byte[read1];
                for (int i = 0; i < read1; i += 2) {
                    short sample1 = (short) ((buffer1[i] & 0xFF) | (buffer1[i + 1] << 8));
                    short sample2 = (short) ((buffer2[i] & 0xFF) | (buffer2[i + 1] << 8));

                    // Điều chỉnh mức âm lượng
                    sample1 *= micVolume;
                    sample2 *= playbackVolume;

                    short mixedSample = (short) Math.min(Math.max(sample1 + sample2, Short.MIN_VALUE), Short.MAX_VALUE);
                    mixedBuffer[i] = (byte) (mixedSample & 0xFF);
                    mixedBuffer[i + 1] = (byte) ((mixedSample >> 8) & 0xFF);
                }
                fos.write(mixedBuffer, 0, read1);
            }

            // Nếu một trong hai tệp vẫn còn dữ liệu
            if (read1 > 0) {
                fos.write(buffer1, 0, read1);
                while ((read1 = fis1.read(buffer1)) > 0) {
                    fos.write(buffer1, 0, read1);
                }
            } else if (read2 > 0) {
                fos.write(buffer2, 0, read2);
                while ((read2 = fis2.read(buffer2)) > 0) {
                    fos.write(buffer2, 0, read2);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}





