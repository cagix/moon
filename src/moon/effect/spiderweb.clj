(ns moon.effect.spiderweb
  (:require [moon.component :refer [defc] :as component]
            [moon.world :refer [timer]]
            [moon.effect :refer [target]]))

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
        [[:tx/apply-modifiers target modifiers]
         [:e/assoc target :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer duration)}]]))))
