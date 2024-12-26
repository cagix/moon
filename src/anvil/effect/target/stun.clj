(ns ^:no-doc anvil.effect.target.stun
  (:require [anvil.component :as component]
            [anvil.entity :as entity]))

(defmethods :effects.target/stun
  (component/info [duration _c]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [[_ duration] {:keys [effect/target]} c]
    (entity/event c target :stun duration)))
