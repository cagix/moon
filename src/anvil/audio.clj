(ns anvil.audio
  (:require [anvil.assets :as assets]
            [clojure.gdx.audio.sound :as sound]))

(defn play-sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets/manager
       sound/play))
