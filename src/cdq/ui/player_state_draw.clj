(ns cdq.ui.player-state-draw
  (:require [cdq.ctx.graphics :as graphics]))

(def state->draw-gui-view)

(defn create [_ctx]
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/graphics
                            ctx/player-eid]
                     :as ctx}]
           (graphics/handle-draws! graphics
                                   (when-let [f (state->draw-gui-view (:state (:entity/fsm @player-eid)))]
                                     (f player-eid ctx))))})
