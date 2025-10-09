(ns clojure.sound)

(defprotocol Sound
  (play! [_])
  (dispose! [_]))
