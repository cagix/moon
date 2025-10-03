(ns cdq.entity.state.player-moving
  (:require [cdq.entity.stats :as stats]))

(defn create [eid movement-vector _world]
  {:movement-vector movement-vector})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/assoc eid :entity/movement {:direction movement-vector
                                    :speed (or (stats/get-stat-value (:entity/stats @eid) :stats/movement-speed)
                                               0)}]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])
