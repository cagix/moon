(ns anvil.effect.target.kill
  (:require [anvil.component :as component]
            [anvil.entity.fsm :as fsm]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/kill
  (component/info [_]
    "Kills target")

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [_ {:keys [effect/target]}]
    (fsm/event target :kill)))
