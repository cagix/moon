(ns cdq.ui.player-state-draw
  (:require [cdq.graphics :as graphics]
            [cdq.entity.state :as state]))

(defn create [_ctx _params]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/world]
                     :as ctx}]
           (let [player-eid (:world/player-eid world)
                 entity @player-eid
                 state-k (:state (:entity/fsm entity))]
             (state/draw-gui-view [state-k (state-k entity)]
                                  player-eid
                                  ctx)))})
