(ns cdq.audio)

(defprotocol Audio
  (dispose! [_])
  (all-sounds [_])
  (play-sound! [_ sound-name]))
