(ns cdq.ui.player-state-draw
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.ui :as ui]))

(defn create []
  (ui/actor
   {:draw (fn [_this]
            (state/draw-gui-view (entity/state-obj @ctx/player-eid)
                                 ctx/player-eid))}))
