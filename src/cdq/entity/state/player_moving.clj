(ns cdq.entity.state.player-moving
  (:require [cdq.stats :as stats]))

(defn create [eid movement-vector _ctx]
  {:movement-vector movement-vector})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/assoc eid :entity/movement {:direction movement-vector
                                    :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                               0)}]])
