(ns cdq.entity.state.player-idle
  (:require [cdq.controls :as controls]
            [cdq.ctx.interaction-state :as interaction-state]
            [cdq.state :as state]
            [gdl.input :as input]
            [cdq.utils :refer [defmethods]]))

(defn handle-input [eid {:keys [ctx/input] :as ctx}]
  (if-let [movement-vector (controls/player-movement-vector ctx)]
    [[:tx/event eid :movement-input movement-vector]]
    (when (input/button-just-pressed? input :left)
      (interaction-state/->txs ctx eid))))

(defmethods :player-idle
  (state/clicked-inventory-cell [_ eid cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
