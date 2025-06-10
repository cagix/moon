(ns gdl.audio)

(defprotocol Sound
  (play! [_]))

(defprotocol Sounds
  (sound [_ path]))

(defprotocol Audio
  (all-sounds [_])
  (play-sound! [_ path]))
