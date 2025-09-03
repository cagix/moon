(ns cdq.tx.sound
  (:require [cdq.audio :as audio]))

(defn do! [[_ sound-name] {:keys [ctx/audio]}]
  (audio/play-sound! audio sound-name)
  nil)
