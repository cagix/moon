(ns cdq.audio.sound
  (:require [cdq.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play! [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets/get
       Sound/.play))
