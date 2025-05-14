(ns cdq.tx.effect
  (:require [cdq.effect :as effect]))

(defn do! [effect-ctx effects]
  (effect/do-all! effect-ctx effects))
