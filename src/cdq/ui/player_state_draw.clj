(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.graphics :as graphics]))

(defn create [_ctx _params]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/graphics
                            ctx/player-eid] :as ctx}]
           (graphics/handle-draws! graphics
                                   (state/draw-gui-view (entity/state-obj @player-eid)
                                                        player-eid
                                                        ctx)))})
