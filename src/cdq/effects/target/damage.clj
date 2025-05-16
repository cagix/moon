(ns cdq.effects.target.damage
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.rand :refer [rand-int-between]]
            [cdq.utils :refer [defcomponent]]))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :entity/armor-save) 0)
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

(defcomponent :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (effect/handle [[_ damage]
                  {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*) ; TODO BUG ALWAYS ARMOR ?
       [[:tx/add-text-effect target "[WHITE]ARMOR"]]

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         [[:tx/assoc-in target [:entity/hp 0] new-hp-val]
          [:tx/audiovisual (:position target*) (db/build ctx/db :audiovisuals/damage)]
          [:tx/event target (if (zero? new-hp-val) :kill :alert)]
          [:tx/add-text-effect target (str "[RED]" dmg-amount "[]")]])))))
