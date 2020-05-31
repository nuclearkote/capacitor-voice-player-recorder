package ru.poseidonnet.cap_voice_player_recorder;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class MyAudioTrack {
    private int mFrequency;// sampling rate
    private int mChannel;// Vocal tract
    private int mSampBit;// Sampling accuracy
    private int format;
    private AudioTrack mAudioTrack;

    public MyAudioTrack(int frequency, int channel, int sampbit, int format) {
        this.mFrequency = frequency;
        this.mChannel = channel;
        this.mSampBit = sampbit;
        this.format = format;
    }

    /**
     * Initialization
     */
    public void init() {
        if (mAudioTrack != null) {
            release();
        }
        // Get the minimum buffer size of the constructed object
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                mFrequency, mChannel, mSampBit, minBufSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    /**
     * Releasing resources
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    /**
     * Write the decoded pcm data to audioTrack for playback
     *
     * @param data   data
     * @param offset deviation
     * @param length Length to play
     */
    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            Log.e("MyAudioTrack", "AudioTrack Exception : " + e.toString());
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, format);
    }
}
