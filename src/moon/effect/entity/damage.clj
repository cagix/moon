(ns moon.effect.entity.damage
  (:require [gdl.rand :refer [rand-int-between]]
            [moon.damage :as damage]
            [moon.db :as db]
            [moon.entity.fsm :as fsm]
            [moon.entity.hp :as hp]
            [moon.entity.stat :as stat]
            [moon.entity.string-effect :as string-effect]
            [moon.world.entities :as entities]))

(defn- effective-armor-save [source* target*]
  (max (- (or (stat/value target* :entity/armor-save) 0)
          (or (stat/value source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

; FIXME no source
(defn info [damage]
  (damage/info damage)
  #_(if source
    (let [modified (damage/modified @source damage)]
      (if (= damage modified)
        (damage/info damage)
        (str (damage/info damage) "\nModified: " (damage/info modified))))
    (damage/info damage))) ; property menu no source,modifiers

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/hp @target)))

(defn handle [damage {:keys [effect/source effect/target]}]
  (let [source* @source
        target* @target
        hp (hp/value target*)]
    (cond
     (zero? (hp 0))
     nil

     (armor-saves? source* target*)
     (swap! target string-effect/add "[WHITE]ARMOR")

     :else
     (let [min-max (:damage/min-max (damage/modified source* target* damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount) 0)]
       (swap! target assoc-in [:entity/hp 0] new-hp-val)
       (entities/audiovisual (:position target*) (db/get :audiovisuals/damage))
       (fsm/event target (if (zero? new-hp-val) :kill :alert))
       (swap! target string-effect/add (str "[RED]" dmg-amount))))))
