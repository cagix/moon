(ns cdq.effects.sound
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _effect-ctx _ctx]
    false)

  (effect/handle [[_ sound] _effect-ctx _ctx]
    [[:tx/sound sound]]))
