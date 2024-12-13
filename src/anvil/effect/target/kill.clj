(ns anvil.effect.target.kill
  (:require [anvil.component :refer [info applicable? handle]]
            [anvil.entity.fsm :as fsm]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/kill
  (info [_]
    "Kills target")

  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [_ {:keys [effect/target]}]
    (fsm/event target :kill)))
