package ru.poseidonnet.cap_voice_player_recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@NativePlugin(
        permissions = {Manifest.permission.MODIFY_AUDIO_SETTINGS}
)
public class VoicePlayer extends Plugin {


    @PluginMethod()
    public void play(PluginCall call) {
        AudioManager audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
        String url = call.getString("url");
        if (url == null || url.trim().isEmpty()) {
            call.resolve(ResponseGenerator.failResponse());
            return;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            call.resolve(ResponseGenerator.successResponse());
        } catch (IOException e) {
            call.resolve(ResponseGenerator.failResponse(e));
        }
    }

}
