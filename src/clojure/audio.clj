(ns clojure.audio)

(defprotocol Audio
  (sound [_ file-handle]))
