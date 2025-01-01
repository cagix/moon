(ns ^:no-doc anvil.effect.target.stun
  (:require [anvil.entity :as entity]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]))

(defcomponent :effects.target/stun
  (component/segment [duration _c]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [[_ duration] {:keys [effect/target]} c]
    (entity/event c target :stun duration)))
