(ns ^:no-doc moon.effects.entity.spiderweb
  (:require [moon.entity.modifiers :as mods]
            [moon.world.time :refer [timer]]))

(def modifiers {:modifier/movement-speed {:op/mult -0.5}})
(def duration 5)

(defn info [_]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defn applicable? [_ _]
  ; ?
  true)

; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
(defn handle [_ {:keys [effect/target]}]
  (when-not (:entity/temp-modifier @target)
    (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                               :counter (timer duration)})
    (swap! target mods/add modifiers)))
