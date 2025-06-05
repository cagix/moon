(ns cdq.entity.state.player-idle
  (:require [cdq.controls :as controls]
            [cdq.ctx :as ctx]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]
            [clojure.x :as x]))

(defcomponent :player-idle
  (state/pause-game? [_] true)

  (state/manual-tick [_ eid ctx]
    (if-let [movement-vector (controls/player-movement-vector ctx)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (ctx/interaction-state ctx eid)]
        (cons [:tx/set-cursor cursor]
              (when (x/button-just-pressed? ctx :left)
                on-click)))))

  (state/clicked-inventory-cell [_ eid cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
