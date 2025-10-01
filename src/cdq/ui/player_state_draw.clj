(ns cdq.ui.player-state-draw
  (:require [cdq.entity.state :as state]))

(defn create []
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/world]
                     :as ctx}]
           (let [player-eid (:world/player-eid world)
                 entity @player-eid
                 state-k (:state (:entity/fsm entity))]
             (state/draw-gui-view [state-k (state-k entity)]
                                  player-eid
                                  ctx)))})
