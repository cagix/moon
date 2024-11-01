(ns moon.effect.entity.spiderweb
  (:require [moon.component :as component]
            [moon.effect :refer [target]]
            [moon.world.time :refer [timer]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defc :effect.entity/spiderweb
    {:schema :some}
    (component/info [_]
      "Spiderweb slows 50% for 5 seconds."
      ; modifiers same like item/modifiers has info-text
      ; counter ?
      )

    (component/applicable? [_]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (component/handle [_]
      (when-not (:entity/temp-modifier @target)
        [[:entity/modifiers target :add modifiers]
         [:e/assoc target :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer duration)}]]))))
