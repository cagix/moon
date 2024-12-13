(ns anvil.effect.target.stun
  (:require [anvil.component :refer [applicable? handle]]
            [anvil.entity.fsm :as fsm]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/stun
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [[_ duration] {:keys [effect/target]}]
    (fsm/event target :stun duration)))
