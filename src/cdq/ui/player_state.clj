(ns cdq.ui.player-state
  (:require [cdq.entity :as entity]
            [cdq.ui :refer [ui-actor]]))

(defn create [_context]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))
