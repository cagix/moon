(ns clojure.audio)

(defprotocol Audio
  (sound [_ path]))
