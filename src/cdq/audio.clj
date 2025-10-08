(ns cdq.audio)

(defprotocol Audio
  (sound-names [_])
  (play! [_ sound-name])
  (dispose! [_]))
