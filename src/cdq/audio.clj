(ns cdq.audio
  (:require [clojure.gdx.audio.sound :as sound]))

(defn sound-names [sounds]
  (map first sounds))

(defn play! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (sound/play! (get sounds sound-name)))

(defn dispose! [sounds]
  (run! sound/dispose! (vals sounds)))
