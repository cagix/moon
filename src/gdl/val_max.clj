(ns gdl.val-max
  (:require [gdl.malli :as m]))

(defn ratio
  "If mx and v is 0, returns 0, otherwise (/ v mx)"
  [[^int v ^int mx]]
  {:pre [(m/validate m/val-max-schema [v mx])]}
  (if (and (zero? v) (zero? mx))
    0
    (/ v mx)))
