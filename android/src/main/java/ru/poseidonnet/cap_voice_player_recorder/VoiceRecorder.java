package ru.poseidonnet.cap_voice_player_recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import static ru.poseidonnet.cap_voice_player_recorder.Messages.ALREADY_RECORDING;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.CANNOT_RECORD_ON_THIS_PHONE;
import static ru.poseidonnet.cap_voice_player_recorder.Messages.FAILED_TO_FETCH_RECORDING;
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

        if (recorder != null) {
            call.reject(ALREADY_RECORDING);
            return;
        }

        int source = call.getInt("source", MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        int bufferLength = call.getInt("bufferLength", 32768);
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
            AudioRecord audioRecord = null;
            try {
                audioRecord = createAudioRecord();
                audioRecord.startRecording();

                while (recording) {
                    handleCodecInput(audioRecord);
                }
            } catch (Exception e) {
                notifyListeners("recordError", ResponseGenerator.failResponse(e));
            } finally {
                if (audioRecord != null) {
                    audioRecord.stop();
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
            return Base64.encodeToString(bArray, Base64.NO_WRAP);
        }

        private void handleCodecInput(AudioRecord audioRecord) {
            byte[] audioRecordData = new byte[bufferLength];
            int length = audioRecord.read(audioRecordData, 0, audioRecordData.length);

            if (length == AudioRecord.ERROR_BAD_VALUE ||
                    length == AudioRecord.ERROR_INVALID_OPERATION ||
                    length != bufferLength) {

                if (length != bufferLength) {
                    notifyListeners("recordError", ResponseGenerator.failResponse());
                    return;
                }
            }
            JSObject data = ResponseGenerator.successResponse();
            data.put("recordDataBase64", toBase64(audioRecordData));
            VoiceRecorder.this.notifyListeners("audioRecorded", data);
        }


        private AudioRecord createAudioRecord() {
            AudioRecord audioRecord = new AudioRecord(source, Parameters.SAMPLE_RATE,
                    Parameters.CHANNELS,
                    Parameters.ENCODING, bufferLength * 10);

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
