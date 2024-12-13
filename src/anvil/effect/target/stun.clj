(ns anvil.effect.target.stun
  (:require [anvil.component :refer [info applicable? handle]]
            [anvil.entity.fsm :as fsm]
            [gdl.utils :refer [defmethods readable-number]]))

(defmethods :effects.target/stun
  (info [duration]
    (str "Stuns for " (readable-number duration) " seconds"))

  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [[_ duration] {:keys [effect/target]}]
    (fsm/event target :stun duration)))
