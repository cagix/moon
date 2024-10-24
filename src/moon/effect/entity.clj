(ns ^:no-doc moon.effect.entity
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [gdl.utils :refer [readable-number]]
            [moon.world :refer [timer stopped? finished-ratio]]
            [moon.entity :as entity]
            [moon.effect :as effect :refer [source target]]))

(defc :entity/temp-modifier
  {:let {:keys [counter modifiers]}}
  (component/info [_]
    (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1[]"))

  (entity/tick [[k _] eid]
    (when (stopped? counter)
      [[:e/dissoc eid k]
       [:tx/reverse-modifiers eid modifiers]]))

  (entity/render-above [_ entity]
    (g/draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defc :effect.entity/spiderweb
    {:schema :some}
    (component/info [_]
      "Spiderweb slows 50% for 5 seconds."
      ; modifiers same like item/modifiers has info-text
      ; counter ?
      )

    (effect/applicable? [_]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (component/handle [_]
      (when-not (:entity/temp-modifier @target)
        [[:tx/apply-modifiers target modifiers]
         [:e/assoc target :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer duration)}]]))))

(defc :effect.entity/convert
  {:schema :some}
  (component/info [_]
    "Converts target to your side.")

  (effect/applicable? [_]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_]
    [[:tx/audiovisual (:position @target) :audiovisuals/convert]
     [:e/assoc target :entity/faction (entity/friend @source)]]))

(defc :effect.entity/stun
  {:schema pos?
   :let duration}
  (component/info [_]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/applicable? [_]
    (and target (:entity/state @target)))

  (component/handle [_]
    [[:tx/event target :stun duration]]))

(defc :effect.entity/kill
  {:schema :some}
  (component/info [_] "Kills target")

  (effect/applicable? [_]
    (and target (:entity/state @target)))

  (component/handle [_]
    [[:tx/event target :kill]]))
