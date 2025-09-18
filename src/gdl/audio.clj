(ns gdl.audio)

(defprotocol Audio
  (sound [_ file-handle]))
