(ns cdq.entity.state.npc-moving
  (:require [cdq.entity :as entity]
            [gdl.context.timer :as timer]
            [cdq.context :as world]))

(defn enter [[_ {:keys [eid movement-vector]}] c]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

(defn exit [[_ {:keys [eid]}] c]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [counter]}] eid c]
  (when (timer/stopped? c counter)
    (world/send-event! c eid :timer-finished)))
