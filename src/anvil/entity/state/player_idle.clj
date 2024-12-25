(ns ^:no-doc anvil.entity.state.player-idle
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [clojure.gdx.audio.sound :as sound]
            [anvil.entity :as entity]
            [anvil.player :as player]
            [gdl.context :as c]
            [gdl.context.db :as db]))

(defmethods :player-idle
  (component/->v [[_ eid]]
    (safe-merge (db/build :player-idle/clicked-inventory-cell)
                {:eid eid}))

  (component/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (controls/movement-vector)]
      (entity/event eid :movement-input movement-vector)
      (let [[cursor on-click] (player/interaction-state eid)]
        (c/set-cursor (c/get-ctx) cursor)
        (when (button-just-pressed? :left)
          (on-click)))))

  (component/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (sound/play pickup-item-sound)
      (entity/event eid :pickup-item item)
      (entity/remove-item eid cell))))
