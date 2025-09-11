(ns cdq.entity.state.npc-moving
  (:require [cdq.stats :as stats]
            [cdq.timer :as timer]))

(def reaction-time-multiplier 0.016)

(defn create [eid movement-vector {:keys [ctx/elapsed-time]}]
  {:movement-vector movement-vector
   :timer (timer/create elapsed-time
                        (* (stats/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                           reaction-time-multiplier))})

(defn tick! [{:keys [timer]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time timer)
    [[:tx/event eid :timer-finished]]))

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/assoc eid :entity/movement {:direction movement-vector
                                    :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                               0)}]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
