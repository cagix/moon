(ns cdq.entity.state.npc-moving
  (:require [cdq.entity.stats :as stats]
            [gdl.timer :as timer]))

(def reaction-time-multiplier 0.016)

(defn create [eid movement-vector {:keys [world/elapsed-time]}]
  {:movement-vector movement-vector
   :timer (timer/create elapsed-time
                        (* (stats/get-stat-value (:entity/stats @eid) :stats/reaction-time)
                           reaction-time-multiplier))})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/assoc eid :entity/movement {:direction movement-vector
                                    :speed (or (stats/get-stat-value (:entity/stats @eid) :stats/movement-speed)
                                               0)}]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
