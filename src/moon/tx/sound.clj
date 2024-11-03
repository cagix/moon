(ns moon.tx.sound
  (:require [gdl.assets :as assets]))

(defn handle [[_ file]]
  (assets/play-sound file)
  nil)
