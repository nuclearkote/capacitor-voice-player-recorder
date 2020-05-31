package ru.poseidonnet.cap_voice_player_recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static ru.poseidonnet.cap_voice_player_recorder.Messages.ALREADY_RECORDING;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.CANNOT_RECORD_ON_THIS_PHONE;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.FAILED_TO_FETCH_RECORDING;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.FAILED_TO_RECORD;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.MISSING_PERMISSION;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.RECORDING_HAS_NOT_STARTED;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.RECORD_AUDIO_REQUEST_CODE;

@NativePlugin(
        permissions = {Manifest.permission.RECORD_AUDIO},
        requestCodes = {RECORD_AUDIO_REQUEST_CODE}
)
public class VoiceRecorder extends Plugin {

    private Recorder recorder;

    @PluginMethod()
    public void canDeviceVoiceRecord(PluginCall call) {
        if (canPhoneCreateMediaRecorder(getContext()))
            call.resolve(ResponseGenerator.successResponse());
        else
            call.resolve(ResponseGenerator.failResponse());
    }

    @PluginMethod()
    public void requestAudioRecordingPermission(PluginCall call) {
        if (doesUserGaveAudioRecordingPermission()) {
            call.resolve(ResponseGenerator.successResponse());
        } else {
            saveCall(call);
            pluginRequestPermission(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO_REQUEST_CODE);
        }
    }

    @PluginMethod()
    public void hasAudioRecordingPermission(PluginCall call) {
        call.resolve(ResponseGenerator.fromBoolean(doesUserGaveAudioRecordingPermission()));
    }

    @PluginMethod()
    public void startRecording(PluginCall call) {
        if (!doesUserGaveAudioRecordingPermission()) {
            call.reject(MISSING_PERMISSION);
            return;
        }

        if (!canPhoneCreateMediaRecorder(getContext())) {
            call.reject(CANNOT_RECORD_ON_THIS_PHONE);
            return;
        }

        if (recorder != null) {
            call.reject(ALREADY_RECORDING);
            return;
        }

        int source = call.getInt("source", MediaRecorder.AudioSource.MIC);
        int bufferLength = call.getInt("bufferLength", 16384);
        recorder = new Recorder(source, bufferLength);
        new Thread(recorder).start();
        call.resolve(ResponseGenerator.successResponse());
    }

    @PluginMethod()
    public void stopRecording(PluginCall call) {
        if (recorder == null) {
            call.reject(RECORDING_HAS_NOT_STARTED);
            return;
        }

        try {
            recorder.stop();
            call.resolve(ResponseGenerator.successResponse());
        } catch (Exception exp) {
            call.reject(FAILED_TO_FETCH_RECORDING, exp);
        } finally {
            recorder = null;
        }
    }

    private static boolean canPhoneCreateMediaRecorder(Context context) {
//        MediaRecorder tempMediaRecorder = null;
//        File outputFile = null;
//        try {
//            File outputDir = context.getCacheDir();
//            outputFile = File.createTempFile("voice_record_temp", ".aac", outputDir);
//            outputFile.deleteOnExit();
//            tempMediaRecorder = new MediaRecorder();
//            tempMediaRecorder.setOutputFile(outputFile.getAbsolutePath());
//            tempMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
//            tempMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            tempMediaRecorder.prepare();
//            tempMediaRecorder.start();
//            return true;
//        } catch (Exception exp) {
//            return exp.getMessage().startsWith("stop failed");
//        } finally {
//            if (outputFile != null) {
//                outputFile.delete();
//            }
//            if (tempMediaRecorder != null) {
//                tempMediaRecorder.stop();
//                tempMediaRecorder.release();
//            }
//        }
        return true;
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        PluginCall savedCall = getSavedCall();
        if (savedCall == null || requestCode != RECORD_AUDIO_REQUEST_CODE)
            return;

        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.resolve(ResponseGenerator.failResponse());
                return;
            }
        }
        savedCall.resolve(ResponseGenerator.successResponse());
    }

    private boolean doesUserGaveAudioRecordingPermission() {
        return hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    private class Recorder implements Runnable {

        final int source;
        boolean recording = true;
        final int bufferLength;

        private Recorder(int source, int bufferLength) {
            this.source = source;
            this.bufferLength = bufferLength;
        }

        @Override
        public void run() {
            MediaCodec mediaCodec = null;
            AudioRecord audioRecord = null;
            try {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                mediaCodec = createMediaCodec();
                mediaCodec.start();
                audioRecord = createAudioRecord();
                audioRecord.startRecording();
                ByteBuffer[] codecInputBuffers = mediaCodec.getInputBuffers();
                ByteBuffer[] codecOutputBuffers = mediaCodec.getOutputBuffers();

                while (recording) {
                    boolean success = handleCodecInput(audioRecord, mediaCodec, codecInputBuffers, recording);
                    if (success) {
                        handleCodecOutput(mediaCodec, codecOutputBuffers, bufferInfo);
                    }
                }
            } catch (IOException e) {
                notifyListeners("recordError", ResponseGenerator.failResponse(e));
            } finally {
                if (mediaCodec != null) {
                    mediaCodec.stop();
                }
                if (audioRecord != null) {
                    audioRecord.stop();
                }

                if (mediaCodec != null) {
                    mediaCodec.release();
                }
                if (audioRecord != null) {
                    audioRecord.release();
                }

            }
        }

        void stop() {
            recording = false;
        }

        private String toBase64(byte[] bArray) {
            return Base64.encodeToString(bArray, Base64.DEFAULT);
        }

        private boolean handleCodecInput(AudioRecord audioRecord,
                                         MediaCodec mediaCodec, ByteBuffer[] codecInputBuffers,
                                         boolean running) {
            byte[] audioRecordData = new byte[bufferLength];
            int length = audioRecord.read(audioRecordData, 0, audioRecordData.length);

            if (length == AudioRecord.ERROR_BAD_VALUE ||
                    length == AudioRecord.ERROR_INVALID_OPERATION ||
                    length != bufferLength) {

                if (length != bufferLength) {
                    return false;
                }
            }

            int codecInputBufferIndex = mediaCodec.dequeueInputBuffer(10 * 1000);

            if (codecInputBufferIndex >= 0) {
                ByteBuffer codecBuffer = codecInputBuffers[codecInputBufferIndex];
                codecBuffer.clear();
                codecBuffer.put(audioRecordData);
                mediaCodec.queueInputBuffer(codecInputBufferIndex, 0, length, 0, running ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }

            return true;
        }

        private void handleCodecOutput(MediaCodec mediaCodec,
                                       ByteBuffer[] codecOutputBuffers,
                                       MediaCodec.BufferInfo bufferInfo)
                throws IOException {
            int codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while (codecOutputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (codecOutputBufferIndex >= 0) {
                        ByteBuffer encoderOutputBuffer = codecOutputBuffers[codecOutputBufferIndex];

                        encoderOutputBuffer.position(bufferInfo.offset);
                        encoderOutputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            byte[] header = createAdtsHeader(bufferInfo.size - bufferInfo.offset);


                            outputStream.write(header);

                            byte[] data = new byte[encoderOutputBuffer.remaining()];
                            encoderOutputBuffer.get(data);
                            outputStream.write(data);
                        }

                        encoderOutputBuffer.clear();

                        mediaCodec.releaseOutputBuffer(codecOutputBufferIndex, false);
                    } else if (codecOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        codecOutputBuffers = mediaCodec.getOutputBuffers();
                    }

                    codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
                JSObject data = ResponseGenerator.successResponse();
                data.put("recordDataBase64", toBase64(outputStream.toByteArray()));
                VoiceRecorder.this.notifyListeners("audioRecorded", data);
            }

        }

        private static final int SAMPLE_RATE = 44100;
        private static final int SAMPLE_RATE_INDEX = 4;
        private static final int CHANNELS = 1;
        private static final int BIT_RATE = 32000;

        private byte[] createAdtsHeader(int length) {
            int frameLength = length + 7;
            byte[] adtsHeader = new byte[7];

            adtsHeader[0] = (byte) 0xFF; // Sync Word
            adtsHeader[1] = (byte) 0xF1; // MPEG-4, Layer (0), No CRC
            adtsHeader[2] = (byte) ((MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1) << 6);
            adtsHeader[2] |= (((byte) SAMPLE_RATE_INDEX) << 2);
            adtsHeader[2] |= (((byte) CHANNELS) >> 2);
            adtsHeader[3] = (byte) (((CHANNELS & 3) << 6) | ((frameLength >> 11) & 0x03));
            adtsHeader[4] = (byte) ((frameLength >> 3) & 0xFF);
            adtsHeader[5] = (byte) (((frameLength & 0x07) << 5) | 0x1f);
            adtsHeader[6] = (byte) 0xFC;

            return adtsHeader;
        }

        private MediaCodec createMediaCodec() throws IOException {
            MediaCodec mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
            MediaFormat mediaFormat = new MediaFormat();

            mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, Parameters.SAMPLE_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, Parameters.CHANNELS);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,  Parameters.BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferLength);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectMain);

            try {
                mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception e) {
                mediaCodec.release();
                throw new IOException(e);
            }

            return mediaCodec;
        }


        private AudioRecord createAudioRecord() {
            AudioRecord audioRecord = new AudioRecord(source, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferLength * 10);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("Unable to initialize AudioRecord");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    android.media.audiofx.NoiseSuppressor noiseSuppressor = android.media.audiofx.NoiseSuppressor
                            .create(audioRecord.getAudioSessionId());
                    if (noiseSuppressor != null) {
                        noiseSuppressor.setEnabled(true);
                    }
                }
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
                    android.media.audiofx.AutomaticGainControl automaticGainControl = android.media.audiofx.AutomaticGainControl
                            .create(audioRecord.getAudioSessionId());
                    if (automaticGainControl != null) {
                        automaticGainControl.setEnabled(true);
                    }
                }
            }


            return audioRecord;
        }
    }

}
