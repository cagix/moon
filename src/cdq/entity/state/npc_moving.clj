(ns cdq.entity.state.npc-moving
  (:require [anvil.entity :as entity]
            [cdq.context :refer [timer stopped?]]))

(defn create [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer c (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defn enter [[_ {:keys [eid movement-vector]}] c]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

(defn exit [[_ {:keys [eid]}] c]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [counter]}] eid c]
  (when (stopped? c counter)
    (entity/event c eid :timer-finished)))
