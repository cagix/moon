(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]))

(defn create [_ctx _params]
  (ui/actor
   {:draw (fn [_this {:keys [ctx/graphics
                             ctx/player-eid] :as ctx}]
            (graphics/handle-draws! graphics
                                    (state/draw-gui-view (entity/state-obj @player-eid)
                                                         player-eid
                                                         ctx)))}))
