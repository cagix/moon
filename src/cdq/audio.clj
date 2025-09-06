(ns cdq.audio
  (:require [clojure.gdx.audio.sound :as sound]))

(defn all-sounds [sounds]
  (map first sounds))

(defn play-sound! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (sound/play! (get sounds sound-name)))
