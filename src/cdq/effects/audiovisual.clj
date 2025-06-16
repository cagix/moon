(ns cdq.effects.audiovisual
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _effect-ctx _world]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]} _world]
    [[:tx/audiovisual target-position audiovisual]]))
