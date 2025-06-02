(ns clojure.effects.target.audiovisual
  (:require [clojure.effect :as effect]
            [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

; creates an audiovisual at the target position
; requires a :effect/target in the effect context

(defcomponent :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _effect-ctx _ctx]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]} _ctx]
    [[:tx/audiovisual (entity/position @target) audiovisual]]))
