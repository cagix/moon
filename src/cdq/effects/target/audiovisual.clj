(ns cdq.effects.target.audiovisual
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _effect-ctx _world]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]} _world]
    [[:tx/audiovisual (entity/position @target) audiovisual]]))
