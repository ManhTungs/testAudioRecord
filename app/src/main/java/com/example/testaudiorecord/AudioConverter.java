package com.example.testaudiorecord;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioConverter {

    static {
        System.loadLibrary("mp3lame");
    }

    private native void initEncoder(int sampleRate, int channels, int bitrate);
    private native int encode(short[] bufferL, short[] bufferR, int samples, byte[] mp3buf);
    private native int flush(byte[] mp3buf);
    private native void closeEncoder();

    public void convertPcmToMp3(String pcmFilePath, String mp3FilePath, int sampleRate, int channels, int bitrate) throws IOException, IOException {
        initEncoder(sampleRate, channels, bitrate);

        FileInputStream fis = new FileInputStream(pcmFilePath);
        FileOutputStream fos = new FileOutputStream(mp3FilePath);

        byte[] buffer = new byte[8192];
        byte[] mp3Buffer = new byte[8192];
        short[] shortBuffer = new short[buffer.length / 2];

        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead / 2; i++) {
                shortBuffer[i] = (short) ((buffer[2 * i + 1] << 8) | (buffer[2 * i] & 0xFF));
            }

            int bytesEncoded = encode(shortBuffer, null, bytesRead / 2, mp3Buffer);
            if (bytesEncoded > 0) {
                fos.write(mp3Buffer, 0, bytesEncoded);
            }
        }

        int outputMp3Bytes = flush(mp3Buffer);
        if (outputMp3Bytes > 0) {
            fos.write(mp3Buffer, 0, outputMp3Bytes);
        }

        fis.close();
        fos.close();
        closeEncoder();
    }
}
