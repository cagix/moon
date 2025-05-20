(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.ui :as ui]))

(defn create []
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid
                             ctx/draw]}]
            (state/draw-gui-view (entity/state-obj @player-eid)
                                 player-eid
                                 draw))}))
