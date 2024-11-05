(ns moon.tx.sound
  (:require [gdl.assets :as assets]))

(defn handle [file]
  (assets/play-sound file)
  nil)
