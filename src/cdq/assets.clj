(ns cdq.assets
  (:require [cdq.audio :as audio]))

(defn sound [assets sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [{:keys [cdq/assets]} sound-name]
  (audio/play (sound assets sound-name)))
