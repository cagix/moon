(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.graphics :as g]
            [cdq.state :as state]
            [gdl.ui :as ui]))

(defn create [_ctx]
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid] :as ctx}]
            (g/handle-draws! ctx
                             (state/draw-gui-view (entity/state-obj @player-eid)
                                                  player-eid
                                                  ctx)))}))
