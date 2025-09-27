(ns cdq.entity.state.player-moving
  (:require [cdq.input :as input]
            [cdq.stats :as stats]))

(defn create [eid movement-vector _world]
  {:movement-vector movement-vector})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/assoc eid :entity/movement {:direction movement-vector
                                    :speed (or (stats/get-stat-value (:entity/stats @eid) :stats/movement-speed)
                                               0)}]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])

(defn- speed [{:keys [entity/stats]}]
  (or (stats/get-stat-value stats :stats/movement-speed)
      0))

(defn handle-input [eid {:keys [ctx/input]}]
  (if-let [movement-vector (input/player-movement-vector input)]
    [[:tx/assoc eid :entity/movement {:direction movement-vector
                                      :speed (speed @eid)}]]
    [[:tx/event eid :no-movement-input]]))
