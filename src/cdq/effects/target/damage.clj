(ns cdq.effects.target.damage
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.modifiers :as modifiers]
            [cdq.rand :refer [rand-int-between]]
            [cdq.utils :refer [defcomponent]]))

; is the problem private functions?
; we are _hiding_ this
; where does this go? entity ?
(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :entity/armor-save)   0)
          (or (entity/stat source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

; What is this defcomponent shit
; a bunch of functions together -> protocol ! defrecord !
; can  use defaults do nil with extend assoc defaults ....
; we want to create records based on keys
; we can do this @ transform ? - based on the namespace
; => we need a structural view also !!
(defcomponent :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (effect/handle [[_ damage]
                  {:keys [effect/source effect/target]}
                  ctx]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*) ; TODO BUG ALWAYS ARMOR ?
       [[:tx/add-text-effect target "[WHITE]ARMOR"]]

       :else
       (let [min-max (:damage/min-max (modifiers/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         [[:tx/assoc-in target [:entity/hp 0] new-hp-val]
          [:tx/audiovisual (entity/position target*) (g/build ctx :audiovisuals/damage)]
          [:tx/event target (if (zero? new-hp-val) :kill :alert)]
          [:tx/add-text-effect target (str "[RED]" dmg-amount "[]")]])))))
