(ns cdq.tx.sound
  (:require [gdl.audio :as audio]))

(defn do! [[_ sound-name] {:keys [ctx/audio]}]
  (->> sound-name
       (format "sounds/%s.wav")
       (audio/play-sound! audio))
  nil)
