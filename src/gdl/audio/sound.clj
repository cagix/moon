(ns gdl.audio.sound)

(defprotocol Sound
  (play! [_])
  (dispose! [_]))
