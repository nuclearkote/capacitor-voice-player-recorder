package ru.poseidonnet.cap_voice_player_recorder;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AACDecoderUtil {


    private static final String TAG = "AACDecoderUtil";
    //Channels
    private static final int KEY_CHANNEL_COUNT = 2;
    //sampling rate
    private static final int KEY_SAMPLE_RATE = 48000;
    //pcm for playback and decoding
    private MyAudioTrack mPlayer;
    //Decoder
    private MediaCodec mDecoder;
    //Number of frames used to record decoding failures
    private int count = 0;

    /**
     * Initialize all variables
     */
    public void start() {
        prepare();
    }

    /**
     * Initial decoder
     *
     * @return Initialization failure returns false and success returns true
     */
    public boolean prepare() {
        // Initialize AudioTrack
        mPlayer = new MyAudioTrack(Parameters.SAMPLE_RATE, Parameters.CHANNELS, Parameters.BIT_RATE, AudioFormat.ENCODING_PCM_16BIT);
        mPlayer.init();
        try {
            //Types of data to be decoded
            String mine = "audio/mp4a-latm";
            //Initial decoder
            mDecoder = MediaCodec.createDecoderByType(mine);
            //MediaFormat is used to describe the relevant parameters of audio and video data
            MediaFormat mediaFormat = new MediaFormat();
            //data type
            mediaFormat.setString(MediaFormat.KEY_MIME, mine);
            //Number of vocal tracts
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, Parameters.CHANNELS);
            //sampling rate
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, Parameters.SAMPLE_RATE);
            //bit rate
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Parameters.BIT_RATE);
            //Used to mark whether AAC has an adts header, 1 - > Yes.
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            //Types used to mark aac
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //ByteBuffer key
            byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            //Decoder configuration
            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (mDecoder == null) {
            return false;
        }
        mDecoder.start();
        return true;
    }

    /**
     * aac Decode + Play
     */
    public void decode(byte[] buf, int offset, int length) {
        //Enter ByteBuffer
        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        //Output ByteBuffer
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
        //Waiting time, 0 - > No waiting, - 1 - > Waiting all the time
        long kTimeOutUs = 0;
        try {
            //Returns an index of an input buffer containing valid data, - 1 - > does not exist
            int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                //Get the current ByteBuffer
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                //Empty ByteBuffer
                dstBuf.clear();
                //Fill in data
                dstBuf.put(buf, offset, length);
                //Submit the input buffer of the specified index to the decoder
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
            }
            //Codec Buffer
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            //Returns an output buffer index, - 1 - > does not exist
            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

            if (outputBufferIndex < 0) {
                //Record the number of decoding failures
                count++;
            }
            ByteBuffer outputBuffer;
            while (outputBufferIndex >= 0) {
                //Get the decoded ByteBuffer
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                //Used to save decoded data
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                //wipe cache
                outputBuffer.clear();
                //Play decoded data
                mPlayer.playAudioTrack(outData, 0, info.size);
                //Release decoded buffer
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                //Decode incomplete data
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    //Returns the number of decoding failures
    public int getCount() {
        return count;
    }

    /**
     * Releasing resources
     */
    public void stop() {
        try {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
