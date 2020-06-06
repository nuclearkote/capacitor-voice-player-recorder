//
//  Created by Avihu Harush on 17/01/2020
//

import Foundation
import AVFoundation

class CustomMediaRecorder {
    
    private let engine = AVAudioEngine()    // instance variable

    private let settings = [
        AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
        AVSampleRateKey: 44100,
        AVNumberOfChannelsKey: 1,
        AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
    ]
    public func startRecording() {
        let input = engine.inputNode
        let bus = 0

        input.installTap(onBus: bus, bufferSize: 262144, format: input.inputFormat(forBus: bus)) { (buffer, time) -> Void in
            let samples = buffer.int32ChannelData?[0]
            // audio callback, samples in samples[0]...samples[buffer.frameLength-1]
        }

        try! engine.start()
    }
    
    public func stopRecording() {
    
    }
    
    
}

