(ns cdq.tx.sound
  (:require [cdq.ctx.audio :as audio]))

(defn do! [[_ sound-name] {:keys [ctx/audio]}]
  (->> sound-name
       (format "%s.wav")
       (audio/play-sound! audio))
  nil)
