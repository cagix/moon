(ns gdl.audio)

(defprotocol Sounds
  (sound [_ path]))

(defprotocol Audio
  (all-sounds [_])
  (play-sound! [_ path]))
