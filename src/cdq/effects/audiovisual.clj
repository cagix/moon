(ns cdq.effects.audiovisual
  (:require [cdq.effect :as effect]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _effect-ctx _ctx]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]} _ctx]
    [[:tx/audiovisual target-position audiovisual]]))
