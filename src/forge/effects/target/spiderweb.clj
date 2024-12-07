(ns forge.effects.target.spiderweb
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]
            [forge.entity.modifiers :as mods]
            [forge.world.time :refer [timer]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defmethods :effects.target/spiderweb
    (applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (handle [_ {:keys [effect/target]}]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer duration)})
        (swap! target mods/add modifiers)))))
