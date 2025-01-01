(ns ^:no-doc anvil.effect.target.kill
  (:require [anvil.entity :as entity]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :effects.target/kill
  (component/segment [_ _c]
    "Kills target")

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [_ {:keys [effect/target]} c]
    (entity/event c target :kill)))
