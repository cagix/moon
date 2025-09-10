(ns cdq.tx.player-remove-skill
  (:require [cdq.stage :as stage]))

(defn do! [[_ skill] {:keys [ctx/stage]}]
  (stage/remove-skill! stage (:property/id skill))
  nil)
