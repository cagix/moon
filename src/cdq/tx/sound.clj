(ns cdq.tx.sound
  (:require [gdl.c :as c]))

(defn do! [ctx sound-name]
  (c/play-sound! ctx sound-name))
