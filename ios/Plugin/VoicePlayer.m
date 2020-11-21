#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(VoicePlayer, "VoicePlayer",
           CAP_PLUGIN_METHOD(play, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(enableSpeakerphone, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(disableSpeakerphone, CAPPluginReturnPromise);
           )
