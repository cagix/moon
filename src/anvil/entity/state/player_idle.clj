(ns ^:no-doc anvil.entity.state.player-idle
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]
            [anvil.player :as player]
            [clojure.gdx.input :refer [button-just-pressed?]]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defmethods :player-idle
  (component/->v [[_ eid]]
    {:eid eid})

  (component/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (controls/movement-vector)]
      (entity/event eid :movement-input movement-vector)
      (let [[cursor on-click] (player/interaction-state eid)]
        (g/set-cursor cursor)
        (when (button-just-pressed? :left)
          (on-click))))))
