(ns cdq.component.manual-tick
  (:require [anvil.controls :as controls]
            [anvil.entity :as entity]
            [anvil.player :as player]
            [cdq.context :as world]
            [clojure.component :as component]
            [clojure.gdx :refer [button-just-pressed?]]
            [gdl.context :as c]))

(defmethod component/manual-tick :player-idle
  [[_ {:keys [eid]}] c]
  (if-let [movement-vector (controls/movement-vector c)]
    (entity/event c eid :movement-input movement-vector)
    (let [[cursor on-click] (player/interaction-state c eid)]
      (c/set-cursor c cursor)
      (when (button-just-pressed? c :left)
        (on-click)))))

(defmethod component/manual-tick :player-item-on-cursor
  [[_ {:keys [eid]}] c]
  (when (and (button-just-pressed? c :left)
             (world/world-item? c))
    (entity/event c eid :drop-item)))
