(ns cdq.ui.player-state-draw
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.c :as c]
            [gdl.ui :as ui]))

(defn create []
  (ui/actor
   {:draw (fn [_this {:keys [ctx/player-eid] :as ctx}]
            (c/handle-draws! ctx
                             (state/draw-gui-view (entity/state-obj @player-eid)
                                                  player-eid
                                                  ctx)))}))
