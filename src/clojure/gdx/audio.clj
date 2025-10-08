(ns clojure.gdx.audio)

(defprotocol Sound
  (play! [_])
  (dispose! [_]))
