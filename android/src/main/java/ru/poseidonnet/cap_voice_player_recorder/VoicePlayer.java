package ru.poseidonnet.cap_voice_player_recorder;

import android.Manifest;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@NativePlugin(
        permissions = {Manifest.permission.MODIFY_AUDIO_SETTINGS}
)
public class VoicePlayer extends Plugin {

    //    private AudioTrack track;
//    private final Deque<byte[]> queue = new LinkedList<>();
//    private int bufferLength;

    @PluginMethod
    public void init(PluginCall call) {
//        try {
        Context context = getContext();
//            bufferLength = call.getInt("bufferLength", 16384);
//            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            audioManager.setMode(AudioManager.STREAM_MUSIC);

//            AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
//            audioFormatBuilder.setSampleRate(Parameters.SAMPLE_RATE);
//            audioFormatBuilder.setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT);
//            audioFormatBuilder.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
//
//            AudioAttributes.Builder aBuilder = new AudioAttributes.Builder();
//            aBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH);
//            track =
//                    new AudioTrack(aBuilder.build(), audioFormatBuilder.build(),
//                            bufferLength * 10, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
//
//            track.play();
//            call.resolve(ResponseGenerator.successResponse());
//        } catch (Exception e) {
//            Log.e(getLogTag(), "Error on init player", e);
//            call.reject("INIT_ERROR", e);
//        }
    }

    @PluginMethod
    public void destroy(PluginCall call) {
    }

    @PluginMethod()
    public void play(PluginCall call) {
//        MediaCodec decoder = null;
//        try {

//            decoder = makeDecoder();
//            decoder.start();
        String base64 = call.getString("base64");
        if (base64.trim().isEmpty()) {
            call.resolve(ResponseGenerator.successResponse());
            return;
        }
        byte[] base64s = readBase64AsByteArray(base64);
//            queue.add(base64s);
//            call.resolve(ResponseGenerator.successResponse());
//        } catch (Exception e) {
//            Log.e(getLogTag(), "Error on playing", e);
//            call.reject("PLAY_ERROR", e);
//        } finally {
//            if (decoder != null) {
//                decoder.stop();
//                decoder.release();
//            }
    }
/*
}


    private MediaCodec makeDecoder() throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, Parameters.SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, Parameters.CHANNELS);
        format.setInteger(MediaFormat.KEY_BIT_RATE, Parameters.BIT_RATE);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferLength);
        //AAC-HE 64kbps
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectMain);
        decoder.configure(format, null, null, 0);

        decoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
                //One InputBuffer is available to decode
                try {
                    Log.v(getLogTag(), "Play init buffer available");
                    while (true) {
                        if (queue.size() == 0) {
                            continue;
                        }

                        byte[] data = queue.removeFirst();
                        Log.v(getLogTag(), "Play buffer got " + data.length);
                        ByteBuffer buffer = mediaCodec.getInputBuffer(i);
                        if (buffer != null) {
                            buffer.put(data, 0, data.length);
                            mediaCodec.queueInputBuffer(i, 0, data.length, 0, 0);
//                            mediaCodec.flush();

                            Log.v(getLogTag(), "Play buffer queued");
                        }
                        break;
                    }
                } catch (Exception e) {
                    Log.e(getLogTag(), "Error in play input buffer", e);
                    notifyListeners("playError", ResponseGenerator.failResponse(e));
                }
            }

            @Override
            public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo info) {
                Log.v(getLogTag(), "Play out buffer available");
                //DECODING PACKET ENDED
                ByteBuffer outBuffer = mediaCodec.getOutputBuffer(i);
                byte[] chunk = new byte[info.size];
                outBuffer.get(chunk); // Read the buffer all at once
                outBuffer.clear();
                track.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data
                mediaCodec.releaseOutputBuffer(i, false);
                mediaCodec.stop();
                mediaCodec.release();
            }

            @Override
            public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
                Log.e(getLogTag(), "Error in play media codec", e);
                notifyListeners("playError", ResponseGenerator.failResponse(e));
            }

            @Override
            public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
                Log.v(getLogTag(), "Play output format changed");
            }
        });

        return decoder;
    }*/

    private byte[] readBase64AsByteArray(String base64) {
        return Base64.decode(base64, Base64.DEFAULT);
    }

}
