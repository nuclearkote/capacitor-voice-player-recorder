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
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(AVAudioSession.Category.playAndRecord,
                                         options:[.mixWithOthers])
        } catch let error as NSError {
            print("audioSession error: \(error.localizedDescription)")
        }
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
        let audioFormat = AVAudioFormat(commonFormat: .pcmFormatInt32, sampleRate: 44100, channels: 1, interleaved: false)
        try! engine.start()

          input.installTap(onBus: 1, bufferSize: 262144, format: audioFormat, block: {
              (buffer: AVAudioPCMBuffer!, time: AVAudioTime!) -> Void in
           do {
                  let data = self.toNSData(PCMBuffer: buffer)
                  let result = data.base64EncodedString()
                  let recordData = RecordData(recordDataBase64: result)
                  self.notifyListeners("audioRecorded", data: recordData.toDictionary())
              } catch {
                  self.notifyListeners("recordError", data: ResponseGenerator.failResponse())
              }
          })
    }

    func toNSData(PCMBuffer: AVAudioPCMBuffer) -> NSData {
        let channelCount = 1  // given PCMBuffer channel count is 1
        let channels = UnsafeBufferPointer(start: PCMBuffer.int32ChannelData, count: channelCount)
        let ch0Data = NSData(bytes: channels[0], length:Int(PCMBuffer.frameCapacity * PCMBuffer.format.streamDescription.pointee.mBytesPerFrame))
        return ch0Data
    }
    
    func doesUserGaveAudioRecordingPermission() -> Bool {
        return AVAudioSession.sharedInstance().recordPermission == AVAudioSession.RecordPermission.granted
    }

}
