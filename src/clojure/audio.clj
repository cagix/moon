(ns clojure.audio
  "
  This interface encapsulates the creation and management of audio resources. It allows you to get direct access to the audio hardware via the AudioDevice and AudioRecorder interfaces, create sound effects via the Sound interface and play music streams via the Music interface.

All resources created via this interface have to be disposed as soon as they are no longer used.

Note that all Music instances will be automatically paused when the ApplicationListener.pause() method is called, and automatically resumed when the ApplicationListener.resume() method is called.
  "
  )

(defprotocol Audio
  (sound [_ file-handle]
         "


Creates a new Sound which is used to play back audio effects such as gun shots or explosions. The Sound's audio data is retrieved from the file specified via the FileHandle. Note that the complete audio data is loaded into RAM. You should therefore not load big audio files with this methods. The current upper limit for decoded audio is 1 MB.

Currently supported formats are WAV, MP3 and OGG.

The Sound has to be disposed if it is no longer used via the Sound.dispose() method.

Returns:
    the new Sound
Throws:
    GdxRuntimeException - in case the sound could not be loaded

         "

         ))
