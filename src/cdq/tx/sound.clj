(ns cdq.tx.sound
  (:require [cdq.audio :as audio]))

(defn do! [{:keys [ctx/audio]} sound-name]
  (audio/play-sound! audio sound-name)
  nil)
