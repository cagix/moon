(ns ^:no-doc anvil.effect.target.kill
  (:require [anvil.component :as component]
            [anvil.entity :as entity]))

(defmethods :effects.target/kill
  (component/info [_]
    "Kills target")

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [_ {:keys [effect/target]} c]
    (entity/event c target :kill)))
