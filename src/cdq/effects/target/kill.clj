(ns cdq.effects.target.kill
  (:require [cdq.effect :as effect]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]} _ctx]
    [[:tx/event target :kill]]))
