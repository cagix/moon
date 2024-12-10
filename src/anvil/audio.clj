(ns anvil.audio
  (:require [anvil.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play-sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets/manager
       Sound/.play))
