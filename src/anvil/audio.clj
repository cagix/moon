(ns anvil.audio
  (:require [anvil.app :as app]
            [clojure.gdx.audio.sound :as sound]))

(defn play-sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       app/asset-manager
       sound/play))
