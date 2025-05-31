(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [gdl.ui :as ui]))

(defn create []
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid] :as ctx}]
            (g/handle-draws! ctx
                             (state/draw-gui-view (entity/state-obj @player-eid)
                                                  player-eid
                                                  ctx)))}))
