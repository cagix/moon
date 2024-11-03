(ns ^:no-doc moon.effect.entity.spiderweb
  (:require [moon.effect :refer [target]]
            [moon.world.time :refer [timer]]))

(def modifiers {:modifier/movement-speed {:op/mult -0.5}})
(def duration 5)

(defn info [_]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defn applicable? [_]
  ; ?
  true)

; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
(defn handle [_]
  (when-not (:entity/temp-modifier @target)
    [[:entity/modifiers target :add modifiers]
     [:e/assoc target :entity/temp-modifier {:modifiers modifiers
                                             :counter (timer duration)}]]))
