(ns cdq.effects.sound
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _effect-ctx _world]
    false)

  (effect/handle [[_ sound] _effect-ctx _world]
    [[:tx/sound sound]]))
