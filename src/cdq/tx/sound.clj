(ns cdq.tx.sound
  (:require [cdq.ctx.audio :as audio]))

(defn do! [[_ sound-name] {:keys [ctx/audio]}]
  (audio/play-sound! audio sound-name)
  nil)
