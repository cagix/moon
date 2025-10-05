(ns clojure.audio.sounds)

(defprotocol Sounds
  (all-names [_])
  (play! [_ sound-name]))
