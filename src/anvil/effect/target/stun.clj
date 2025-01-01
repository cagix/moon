(ns ^:no-doc anvil.effect.target.stun
  (:require [gdl.effect.component :as component]
            [anvil.entity :as entity]
            [gdl.info :as info]
            [clojure.component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]))

(defcomponent :effects.target/stun
  (info/segment [duration _c]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [[_ duration] {:keys [effect/target]} c]
    (entity/event c target :stun duration)))
