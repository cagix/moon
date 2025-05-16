(ns cdq.effects.audiovisual
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]}]
    [[:tx/audiovisual target-position audiovisual]]))
