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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class Service3 extends Service {

    String pathPlayBack = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioPlayback.pcm";
    String pathMic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioMic.pcm";
    String pathMerge = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioMerge.pcm";
    String pathWav = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath() + "/test_music_haha/audioMerge.wav";
    String pathMp3 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/sample-6s.mp3";

    String pathVideo = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath() + "/EZRecorder/SD2024-05-30-16-18-51.mp4";
    String pathResult = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath() + "/EZRecorder/hiuhiuuuu.mp4";

    public static final String CHANNEL_ID = "record_channel";
    private static CharSequence CHANNEL_NAME = "record_channel_name";
    public static final int NOTIFICATION_ID = 1;

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private static final int BUFFER_SIZE_CONVERT = AudioRecord.getMinBufferSize(8000,
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

    final int bpp = 16;
    int sampleRate = 44100;

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
                case "WAV":
//                    createWavFile(pathMerge,pathWav);
                    createWavFile(pathMerge,getFilesDir()+"/hahaha123.wav");
                    break;
                case "PLAY_WAV":
                   playWavFile(pathWav);
                    break;
                case "WAV_MP4":
                    try {
                        muxWavAndMp4(pathMp3,pathVideo,pathResult);
                    } catch (IOException e) {
                        Log.e("dfdf", "onStartCommand: ",e );
                    }
                    break;
            }
        }

        return START_STICKY;
    }

    private void playWavFile(String filePath) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);

            // Read WAV file header
            byte[] header = new byte[44];
            if (fis.read(header, 0, 44) != 44) {
                Log.e("dfdf", "Error reading WAV file header");
                return;
            }

            // Extract audio format information from header
            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            audioTrack.play();

            while ((bytesRead = fis.read(buffer)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }

            audioTrack.stop();
            audioTrack.release();
        } catch (IOException e) {
            Log.e("dfdf", "Error playing WAV file", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e("dfdf", "Error closing file input stream", e);
                }
            }
        }
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

    private void createWavFile(String tempPath, String wavPath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(tempPath);
            FileOutputStream fileOutputStream = new FileOutputStream(wavPath);
            byte[] data = new byte[BUFFER_SIZE_CONVERT];
            int channels = 2;
            long byteRate = (long) bpp * sampleRate * channels / 8;
            long totalAudioLen = fileInputStream.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            wavHeader(fileOutputStream, totalAudioLen, totalDataLen, channels, byteRate);
            while (fileInputStream.read(data) != -1) {
                fileOutputStream.write(data);
            }
            fileInputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wavHeader(FileOutputStream fileOutputStream, long totalAudioLen, long totalDataLen, int channels, long byteRate) {
        Log.e("dfdf", "wavHeader: "+byteRate);
        try {
            byte[] header = new byte[44];
            header[0] = 'R'; // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f'; // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16; // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1; // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) ((long) sampleRate & 0xff);
            header[25] = (byte) (((long) sampleRate >> 8) & 0xff);
            header[26] = (byte) (((long) sampleRate >> 16) & 0xff);
            header[27] = (byte) (((long) sampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (2 * 16 / 8); // block align
            header[33] = 0;
            header[34] = bpp; // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
            fileOutputStream.write(header, 0, 44);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void muxWavAndMp4(String wavFilePath, String mp4FilePath, String outputFilePath) throws IOException {
        MediaMuxer mediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaExtractor videoExtractor = new MediaExtractor();
        int videoTrackIndex = selectTrack(videoExtractor, "video/");

        MediaExtractor audioExtractor = new MediaExtractor();
        int audioTrackIndex=selectTrack(audioExtractor,"audio/wav");
        MediaFormat audioFormat = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            audioFormat = new MediaFormat(audioExtractor.getTrackFormat(audioTrackIndex));
            MediaFormat videoFormat = new MediaFormat(videoExtractor.getTrackFormat(videoTrackIndex));

        }
//        int audioTrackIndex = mediaMuxer.addTrack(audioFormat);
//        int videoTrackIndex = mediaMuxer.addTrack(videoFormat);

        // Setup Metadata Track
//        MediaFormat metadataFormat = new MediaFormat(...);
//        metadataFormat.setString(KEY_MIME, "application/gyro");
//        int metadataTrackIndex = mediaMuxer.addTrack(metadataFormat);

//        MediaExtractor videoExtractor = new MediaExtractor();
//        videoExtractor.setDataSource(mp4FilePath);
//        int videoTrackIndex = selectTrack(videoExtractor, "video/");
//        MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
//
//        MediaExtractor audioExtractor = new MediaExtractor();
//        audioExtractor.setDataSource(wavFilePath);
//        int audioTrackIndex = selectTrack(audioExtractor, "audio/");
//        MediaFormat audioFormat = audioExtractor.getTrackFormat(audioTrackIndex);
//
//        MediaMuxer mediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        int muxerVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
//        int muxerAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
//
//        mediaMuxer.start();
//
//        // Write video data
//        videoExtractor.selectTrack(videoTrackIndex);
//        ByteBuffer videoBuffer = ByteBuffer.allocate(1024 * 1024);
//        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
//        while (true) {
//            int sampleSize = videoExtractor.readSampleData(videoBuffer, 0);
//            if (sampleSize < 0) {
//                break;
//            }
//            videoBufferInfo.offset = 0;
//            videoBufferInfo.size = sampleSize;
//            videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
//            videoBufferInfo.flags = videoExtractor.getSampleFlags();
//            mediaMuxer.writeSampleData(muxerVideoTrackIndex, videoBuffer, videoBufferInfo);
//            videoExtractor.advance();
//        }
//
//        // Write audio data
//        audioExtractor.selectTrack(audioTrackIndex);
//        ByteBuffer audioBuffer = ByteBuffer.allocate(1024 * 1024);
//        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
//        while (true) {
//            int sampleSize = audioExtractor.readSampleData(audioBuffer, 0);
//            if (sampleSize < 0) {
//                break;
//            }
//            audioBufferInfo.offset = 0;
//            audioBufferInfo.size = sampleSize;
//            audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
//            audioBufferInfo.flags = audioExtractor.getSampleFlags();
//            mediaMuxer.writeSampleData(muxerAudioTrackIndex, audioBuffer, audioBufferInfo);
//            audioExtractor.advance();
//        }
//
//        mediaMuxer.stop();
//        mediaMuxer.release();
//        videoExtractor.release();
//        audioExtractor.release();
    }

    private int selectTrack(MediaExtractor extractor, String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

}





