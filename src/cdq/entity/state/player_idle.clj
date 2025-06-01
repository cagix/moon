(ns cdq.entity.state.player-idle
  (:require [cdq.controls :as controls]
            [cdq.g :as g]
            [cdq.input :as input]
            [cdq.state :as state]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :player-idle
  (state/pause-game? [_] true)

  (state/manual-tick [_ eid ctx]
    (if-let [movement-vector (controls/player-movement-vector ctx)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (g/interaction-state ctx eid)]
        (cons [:tx/set-cursor cursor]
              (when (input/button-just-pressed? ctx :left)
                on-click)))))

  (state/clicked-inventory-cell [_ eid cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
