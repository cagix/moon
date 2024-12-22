(ns ^:no-doc anvil.entity.state.player-idle
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]
            [clojure.gdx.input :refer [button-just-pressed?]]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defn interaction-state [eid])

(defmethods :player-idle
  (component/->v [[_ eid]]
    {:eid eid})

  (component/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (controls/movement-vector)]
      (entity/event eid :movement-input movement-vector)
      (let [[cursor on-click] (interaction-state eid)]
        (g/set-cursor cursor)
        (when (button-just-pressed? :left)
          (on-click))))))
