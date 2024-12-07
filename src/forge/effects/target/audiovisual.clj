(ns forge.effects.target.audiovisual
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle useful?]]
            [forge.world :refer [spawn-audiovisual]]))

(defmethods :effects.target/audiovisual
  (applicable? [_ {:keys [effect/target]}]
    target)

  (useful? [_ _]
    false)

  (handle [[_ audiovisual] {:keys [effect/target]}]
    (spawn-audiovisual (:position @target) audiovisual)))
