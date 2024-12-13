(ns anvil.effect.target.audiovisual
  (:require [anvil.component :refer [applicable? useful? handle]]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/audiovisual
  (applicable? [_ {:keys [effect/target]}]
    target)

  (useful? [_ _]
    false)

  (handle [[_ audiovisual] {:keys [effect/target]}]
    (world/audiovisual (:position @target) audiovisual)))
