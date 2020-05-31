
  Pod::Spec.new do |s|
    s.name = 'CapacitorVoicePlayerRecorder'
    s.version = '0.0.1'
    s.summary = 'Capacitor plugin for voice recording and playing'
    s.license = 'MIT'
    s.homepage = 'https://github.com/nuclearkote/capacitor-voice-player-recorder'
    s.author = 'nuclearkote'
    s.source = { :git => 'https://github.com/nuclearkote/capacitor-voice-player-recorder', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end
