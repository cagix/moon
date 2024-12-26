(ns ^:no-doc anvil.entity.state.player-idle
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [clojure.gdx :refer [button-just-pressed?]]
            [clojure.gdx.audio.sound :as sound]
            [anvil.entity :as entity]
            [anvil.player :as player]
            [gdl.context :as c]))

(defmethods :player-idle
  (component/->v [[_ eid] c]
    (safe-merge (c/build c :player-idle/clicked-inventory-cell)
                {:eid eid}))

  (component/manual-tick [[_ {:keys [eid]}] c]
    (if-let [movement-vector (controls/movement-vector c)]
      (entity/event c eid :movement-input movement-vector)
      (let [[cursor on-click] (player/interaction-state c eid)]
        (c/set-cursor c cursor)
        (when (button-just-pressed? c :left)
          (on-click)))))

  (component/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (sound/play pickup-item-sound)
      (entity/event c eid :pickup-item item)
      (entity/remove-item c eid cell))))
