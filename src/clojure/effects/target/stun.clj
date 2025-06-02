(ns clojure.effects.target.stun
  (:require [clojure.effect :as effect]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]} _ctx]
    [[:tx/event target :stun duration]]))
