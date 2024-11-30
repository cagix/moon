(ns ^:no-doc forge.effects.target.damage
  (:require [forge.db :as db]
            [forge.math.rand :refer [rand-int-between]]
            [forge.entity.components :as entity]
            [forge.world :as world]))

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

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/hp @target)))

(defn handle [damage {:keys [effect/source effect/target]}]
  (let [source* @source
        target* @target
        hp (entity/hitpoints target*)]
    (cond
     (zero? (hp 0))
     nil

     (armor-saves? source* target*)
     (swap! target entity/add-text-effect "[WHITE]ARMOR")

     :else
     (let [min-max (:damage/min-max (entity/damage-mods source* target* damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount) 0)]
       (swap! target assoc-in [:entity/hp 0] new-hp-val)
       (world/audiovisual (:position target*) (db/build :audiovisuals/damage))
       (entity/event target (if (zero? new-hp-val) :kill :alert))
       (swap! target entity/add-text-effect (str "[RED]" dmg-amount "[]"))))))
