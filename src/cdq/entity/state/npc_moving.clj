(ns cdq.entity.state.npc-moving
  (:require [cdq.world.entity.stats :as modifiers]
            [cdq.timer :as timer]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :timer-finished]]))

(defn create [eid movement-vector {:keys [ctx/elapsed-time]}]
  {:movement-vector movement-vector
   :counter (timer/create elapsed-time
                          (* (modifiers/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                             0.016))})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/set-movement eid movement-vector]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
