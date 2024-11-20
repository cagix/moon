(ns ^:no-doc methods.effects.target.spiderweb
  (:require [moon.entity :as entity]
            [moon.world :refer [timer]]))

(def modifiers {:modifier/movement-speed {:op/mult -0.5}})
(def duration 5)

(defn applicable? [_ _]
  ; ?
  true)

; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
(defn handle [_ {:keys [effect/target]}]
  (when-not (:entity/temp-modifier @target)
    (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                               :counter (timer duration)})
    (swap! target entity/add-mods modifiers)))
