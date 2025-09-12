(ns cdq.tx.player-remove-skill
  (:require [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   skill]
  (stage/remove-skill! stage (:property/id skill))
  nil)
