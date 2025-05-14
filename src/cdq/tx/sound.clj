(ns cdq.tx.sound
  (:require [cdq.audio.sound :as sound]))

(defn do! [sound-name]
  (sound/play! sound-name))
