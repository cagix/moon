(ns forge.effects.target.damage
  (:require [anvil.damage :as damage]
            [anvil.db :as db]
            [anvil.fsm :as fsm]
            [anvil.hitpoints :as hp]
            [anvil.stat :as stat]
            [anvil.string-effect :as string-effect]
            [anvil.world :refer [spawn-audiovisual]]
            [clojure.rand :refer [rand-int-between]]))

(defn- effective-armor-save [source* target*]
  (max (- (or (stat/->value target* :entity/armor-save) 0)
          (or (stat/->value source* :entity/armor-pierce) 0))
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

(defn handle [[_ damage] {:keys [effect/source effect/target]}]
  (let [source* @source
        target* @target
        hp (hp/->value target*)]
    (cond
     (zero? (hp 0))
     nil

     (armor-saves? source* target*)
     (swap! target string-effect/add "[WHITE]ARMOR")

     :else
     (let [min-max (:damage/min-max (damage/->value source* target* damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount) 0)]
       (swap! target assoc-in [:entity/hp 0] new-hp-val)
       (spawn-audiovisual (:position target*)
                          (db/build :audiovisuals/damage))
       (fsm/event target (if (zero? new-hp-val) :kill :alert))
       (swap! target string-effect/add (str "[RED]" dmg-amount "[]"))))))
