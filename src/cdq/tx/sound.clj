(ns cdq.tx.sound
  (:require [cdq.ctx.audio :as audio]))

(defn do!
  [{:keys [ctx/audio]}
   sound-name]
  (audio/play-sound! audio sound-name)
  nil)
