(ns cdq.assets
  (:require [gdl.audio.sound :as sound]))

(defn sound [assets sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [{:keys [cdq/assets]} sound-name]
  (sound/play (sound assets sound-name)))
