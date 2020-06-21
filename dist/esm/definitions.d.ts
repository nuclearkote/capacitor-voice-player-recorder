declare module '@capacitor/core' {
    interface PluginRegistry {
        VoiceRecorder: VoiceRecorderPlugin;
        VoicePlayer: VoicePlayerPlugin;
    }
}
export declare type Base64String = string;
export interface RecordingData {
    recordDataBase64: Base64String;
}
export interface GenericResponse {
    value: boolean;
    message: string;
}
export interface VoicePlayerPlugin {
    play(options: PlayOptions): Promise<GenericResponse>;
}
export interface VoiceRecorderPlugin {
    canDeviceVoiceRecord(): Promise<GenericResponse>;
    requestAudioRecordingPermission(): Promise<GenericResponse>;
    hasAudioRecordingPermission(): Promise<GenericResponse>;
    startRecording(options?: RecordOptions): Promise<GenericResponse>;
    stopRecording(): Promise<GenericResponse>;
}
export interface RecordOptions {
    sampleRate?: number;
    bufferLength?: number;
    source?: RecordSource;
}
export interface PlayOptions {
    url: string;
}
export declare enum RecordSource {
    DEFAULT = 0,
    MIC = 1,
    VOICE_UPLINK = 2,
    VOICE_DOWNLINK = 3,
    VOICE_CALL = 4,
    CAMCORDER = 5,
    VOICE_RECOGNITION = 6,
    VOICE_COMMUNICATION = 7,
    REMOTE_SUBMIX = 8,
    UNPROCESSED = 9,
    RADIO_TUNER = 1998,
    HOTWORD = 1999
}
