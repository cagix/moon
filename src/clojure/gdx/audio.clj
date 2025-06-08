(ns clojure.gdx.audio)

(defprotocol Audio
  (sound [_ file-handle]))
