(ns clojure.audio.sound
  "A Sound is a short audio clip that can be played numerous times in parallel. It's completely loaded into memory so only load
  small audio files. Call the [[clojure.disposable/dispose!]] function when you're done using the Sound.

  Sound instances are created via a call to [[clojure.audio/sound]].

  Calling [[play!]] will return a long which is an id to that instance of the sound. You
  can use this id to modify the playback of that sound instance.

  <b>Note</b>: any values provided will not be clamped, it is the developer's responsibility to do so.")

(defprotocol Sound
  (play! [_]
         "Plays the sound. If the sound is already playing, it will be played again, concurrently

         Returns:
         the id of the sound instance if successful, or -1 on failure."))
