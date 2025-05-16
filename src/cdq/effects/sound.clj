(ns cdq.effects.sound
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _]
    false)

  (effect/handle [[_ sound] _ctx]
    [[:tx/sound sound]]))
