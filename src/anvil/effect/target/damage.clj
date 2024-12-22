(ns ^:no-doc anvil.effect.target.damage
  (:require [anvil.component :as component]
            [anvil.db :as db]
            [anvil.entity :as entity]
            [anvil.world :as world :refer [add-text-effect]]
            [gdl.rand :refer [rand-int-between]]))

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

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

(defmethods :effects.target/damage
  (component/info [[_ damage]]
    (damage-info damage)
    #_(if source
        (let [modified (entity/damage @source damage)]
          (if (= damage modified)
            (damage-info damage)
            (str (damage-info damage) "\nModified: " (damage/info modified))))
        (damage-info damage)) ; property menu no source,modifiers
    )

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (component/handle [[_ damage] {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target add-text-effect "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (world/audiovisual (:position target*)
                            (db/build :audiovisuals/damage))
         (entity/event target (if (zero? new-hp-val) :kill :alert))
         (swap! target add-text-effect (str "[RED]" dmg-amount "[]")))))))
