(ns cdq.create.stage.player-state
  (:require [cdq.entity :as entity]
            [gdl.ui :refer [ui-actor]]))

(defn create [_context]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))
