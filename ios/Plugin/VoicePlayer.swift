import Foundation
import Capacitor
import AVFoundation

@objc(VoicePlayer)
public class VoicePlayer: CAPPlugin {

    var queuePlayer = AVQueuePlayer()
    var started = false

    @objc func play(_ call: CAPPluginCall) {

        guard let audioUrl = call.getString("url") else { return  }
        print(audioUrl)
        guard let url = URL.init(string: audioUrl) else { return }
        let playerItem = AVPlayerItem.init(url: url)
        if (!started) {

           started = true
            queuePlayer.automaticallyWaitsToMinimizeStalling = false
            queuePlayer.playImmediately(atRate: 1.0)

        }
        queuePlayer.insert(playerItem, after: nil)

        do {
            try AVAudioSession.sharedInstance().setPreferredIOBufferDuration(0.002)
            try AVAudioSession.sharedInstance().setCategory(.playAndRecord, options: [.mixWithOthers])
            print("Playback OK")
            try AVAudioSession.sharedInstance().setActive(true)
            print("Session is Active")
        } catch {
          print(error)
        }
        call.resolve(ResponseGenerator.successResponse())
    }
    
}
