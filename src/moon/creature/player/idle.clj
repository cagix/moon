(ns ^:no-doc moon.creature.player.idle
  (:require [gdl.input :refer [button-just-pressed? WASD-movement-vector]]
            [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.entity.state :as state]))

(defc :player-idle
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (state/pause-game? [_]
    true)

  (state/manual-tick [_]
    (if-let [movement-vector (WASD-movement-vector)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (entity/interaction-state eid)]
        (cons [:tx/cursor cursor]
              (when (button-just-pressed? :left)
                (on-click))))))

  (state/clicked-inventory-cell [_ cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "sounds/bfxr_takeit.wav"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]]))

  (state/clicked-skillmenu-skill [_ skill]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (entity/has-skill? @eid skill)))
        [[:e/assoc eid :entity/free-skill-points (dec free-skill-points)]
         [:tx/add-skill eid skill]]))))
