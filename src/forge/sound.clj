(ns forge.sound
  (:require [forge.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(def asset-format "sounds/%s.wav")

(defn play [sound-name]
  (->> sound-name
       (format asset-format)
       assets/manager
       Sound/.play))
