(ns cdq.entity.state.player-idle
  (:require [cdq.g :as g]
            [cdq.state :as state]
            [gdl.input :as input]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :player-idle
  (state/pause-game? [_] true)

  (state/manual-tick [_ eid {:keys [ctx/input]
                             :as ctx}]
    (if-let [movement-vector (g/player-movement-vector ctx)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (g/interaction-state ctx eid)]
        (cons [:tx/set-cursor cursor]
              (when (input/button-just-pressed? input :left)
                on-click)))))

  (state/clicked-inventory-cell [_ eid cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
