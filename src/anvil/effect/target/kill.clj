(ns ^:no-doc anvil.effect.target.kill
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [gdl.info :as info]
            [clojure.utils :refer [defmethods]]))

(defmethods :effects.target/kill
  (info/segment [_ _c]
    "Kills target")

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [_ {:keys [effect/target]} c]
    (entity/event c target :kill)))
