(ns anvil.effect.target.stun
  (:require [anvil.component :as component]
            [anvil.entity.fsm :as fsm]
            [gdl.utils :refer [defmethods readable-number]]))

(defmethods :effects.target/stun
  (component/info [duration]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [[_ duration] {:keys [effect/target]}]
    (fsm/event target :stun duration)))
