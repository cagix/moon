(ns clojure.assets
  (:require [clojure.audio :as audio]))

(defn sound [assets sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [{:keys [clojure/assets]} sound-name]
  (audio/play (sound assets sound-name)))
