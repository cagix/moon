(ns cdq.entity.state.player-moving.handle-input
  (:require [cdq.input :as input]
            [cdq.entity.stats :as stats]))

(defn- speed [{:keys [entity/stats]}]
  (or (stats/get-stat-value stats :stats/movement-speed)
      0))

(defn txs [eid {:keys [ctx/gdx]}]
  (if-let [movement-vector (input/player-movement-vector gdx)]
    [[:tx/assoc eid :entity/movement {:direction movement-vector
                                      :speed (speed @eid)}]]
    [[:tx/event eid :no-movement-input]]))
