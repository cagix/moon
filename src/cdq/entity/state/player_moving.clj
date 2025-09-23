(ns cdq.entity.state.player-moving
  (:require [cdq.input :as input]
            [cdq.stats :as stats]))

(defn- speed [{:keys [creature/stats]}]
  (or (stats/get-stat-value stats :entity/movement-speed)
      0))

(defn handle-input [eid {:keys [ctx/input]}]
  (if-let [movement-vector (input/player-movement-vector input)]
    [[:tx/assoc eid :entity/movement {:direction movement-vector
                                      :speed (speed @eid)}]]
    [[:tx/event eid :no-movement-input]]))
