(ns cdq.entity.state.npc-moving
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/event eid :timer-finished]]))

(defn create [eid movement-vector {:keys [ctx/world]}]
  {:movement-vector movement-vector
   :counter (timer/create (:world/elapsed-time world) (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/set-movement eid movement-vector]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
