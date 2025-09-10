(ns cdq.tx.player-remove-skill
  (:require [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   skill]
  (stage/remove-skill! stage (:property/id skill))
  nil)
