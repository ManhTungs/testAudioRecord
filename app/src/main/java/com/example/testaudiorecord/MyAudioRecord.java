package com.example.testaudiorecord;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.FileOutputStream;
import java.io.IOException;

public class MyAudioRecord {
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Context context;

    public MyAudioRecord(Context context) {
        this.context = context;
    }

    public void startRecording(String filePath, float volumeFactor) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed.");
            return;
        }



        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            try (FileOutputStream os = new FileOutputStream(filePath)) {
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        adjustVolume(buffer, volumeFactor);
                        os.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Recording failed", e);
            }
        }).start();
    }

    public void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void adjustVolume(byte[] audioData, float volumeFactor) {
        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short)((audioData[i] & 0xFF) | (audioData[i + 1] << 8));
            int adjustedSample = (int)(sample * volumeFactor);
            if (adjustedSample > Short.MAX_VALUE) {
                adjustedSample = Short.MAX_VALUE;
            } else if (adjustedSample < Short.MIN_VALUE) {
                adjustedSample = Short.MIN_VALUE;
            }
            audioData[i] = (byte)(adjustedSample & 0xFF);
            audioData[i + 1] = (byte)((adjustedSample >> 8) & 0xFF);
        }
    }
}
