(ns cdq.audio)

(defprotocol Audio
  (all-sounds [_])
  (play-sound! [_ path]))
