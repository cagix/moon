(ns cdq.effects.target.audiovisual
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _effect-ctx _ctx]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]} _ctx]
    [[:tx/audiovisual (:position @target) audiovisual]]))
