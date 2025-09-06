(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]
            [clojure.gdx.input :as input]
            [cdq.controls :as controls]
            [cdq.stats :as stats]
            [cdq.entity.state.player-idle]))

(defn- speed [{:keys [creature/stats]}]
  (or (stats/get-stat-value stats :entity/movement-speed)
      0))

(def state->handle-input
  {:player-idle           (fn [player-eid {:keys [ctx/input] :as ctx}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/event player-eid :movement-input movement-vector]]
                              (when (input/button-just-pressed? input :left)
                                (cdq.entity.state.player-idle/interaction-state->txs ctx player-eid))))
   :player-item-on-cursor (fn [eid
                               {:keys [ctx/input
                                       ctx/mouseover-actor]}]
                            (when (and (input/button-just-pressed? input :left)
                                       (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:tx/event eid :drop-item]]))
   :player-moving         (fn [eid {:keys [ctx/input]}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                :speed (speed @eid)}]]
                              [[:tx/event eid :no-movement-input]]))})

(defn do!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  nil)
