(ns cdq.entity.state.player-idle
  (:require [cdq.controls :as controls]
            [cdq.ctx :as ctx]
            [cdq.state :as state]
            [gdl.input :as input]
            [cdq.utils :refer [defmethods]]))

(defmethods :player-idle
  (state/cursor [_ eid ctx]
    (let [[cursor _on-click] (ctx/interaction-state ctx eid)]
      cursor))

  (state/manual-tick [_ eid {:keys [ctx/input] :as ctx}]
    (if-let [movement-vector (controls/player-movement-vector ctx)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[_cursor on-click] (ctx/interaction-state ctx eid)]
        (when (input/button-just-pressed? input :left)
          on-click))))

  (state/clicked-inventory-cell [_ eid cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
