(ns cdq.ctx.audio)

(defprotocol Audio
  (all-sounds [_])
  (play-sound! [_ sound-name]))
