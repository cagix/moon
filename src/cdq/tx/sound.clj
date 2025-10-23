(ns cdq.tx.sound
  (:require [cdq.audio :as audio]))

(defn do!
  [{:keys [ctx/audio] :as ctx} sound-name]
  (audio/play! audio sound-name)
  ctx)
