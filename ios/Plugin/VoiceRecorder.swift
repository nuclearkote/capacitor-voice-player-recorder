import Foundation
import Capacitor
import AVFoundation

@objc(VoiceRecorder)
public class VoiceRecorder: CAPPlugin {
    
    private var recording: Bool = false;
    
    private let engine = AVAudioEngine()    // instance variable

    @objc func requestAudioRecordingPermission(_ call: CAPPluginCall) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            if granted {
                call.resolve(ResponseGenerator.successResponse())
            } else {
                call.resolve(ResponseGenerator.failResponse())
            }
        }
    }
    
    @objc func hasAudioRecordingPermission(_ call: CAPPluginCall) {
        call.resolve(ResponseGenerator.fromBoolean(doesUserGaveAudioRecordingPermission()))
    }
    
    
    @objc func startRecording(_ call: CAPPluginCall) {
        if(!doesUserGaveAudioRecordingPermission()) {
            call.reject(Messages.MISSING_PERMISSION)
            return
        }
        
        if(recording) {
            call.reject(Messages.ALREADY_RECORDING)
            return
        }
        recording = true
        start()
        call.resolve(ResponseGenerator.successResponse())
    }
    
    @objc func stopRecording(_ call: CAPPluginCall) {
        if (!recording) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
            return
        }
         try! engine.stop()
    }
    
    func start() {
       let input = engine.inputNode
       let bus = 0

       input.installTap(onBus: bus, bufferSize: 262144, format: input.inputFormat(forBus: bus)) { (buffer, time) -> Void in
        let data = self.toNSData(PCMBuffer: buffer)
        let result = data.base64EncodedString()
        let recordData = RecordData(recordDataBase64: result)
        self.notifyListeners("audioRecorded", data: recordData.toDictionary())
           // audio callback, samples in samples[0]...samples[buffer.frameLength-1]
       }

       try! engine.start()
    }
    
    func toNSData(PCMBuffer: AVAudioPCMBuffer) -> NSData {
        let channelCount = 1  // given PCMBuffer channel count is 1
        let channels = UnsafeBufferPointer(start: PCMBuffer.floatChannelData, count: channelCount)
        let ch0Data = NSData(bytes: channels[0], length:Int(PCMBuffer.frameCapacity * PCMBuffer.format.streamDescription.pointee.mBytesPerFrame))
        return ch0Data
    }
    
    func doesUserGaveAudioRecordingPermission() -> Bool {
        return AVAudioSession.sharedInstance().recordPermission == AVAudioSession.RecordPermission.granted
    }
    
}
