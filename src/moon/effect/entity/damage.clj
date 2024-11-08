(ns moon.effect.entity.damage
  (:require [gdl.rand :refer [rand-int-between]]
            [moon.audiovisual :as audiovisual]
            [moon.damage :as damage]
            [moon.effect :refer [source target]]
            [moon.entity.hp :as hp]
            [moon.entity.stat :as stat]))

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

(defn info [damage]
  (if source
    (let [modified (damage/modified @source damage)]
      (if (= damage modified)
        (damage/info damage)
        (str (damage/info damage) "\nModified: " (damage/info modified))))
    (damage/info damage))) ; property menu no source,modifiers

(defn applicable? [_]
  (and target
       (:entity/hp @target)))

(defn handle [damage]
  (let [source* @source
        target* @target
        hp (hp/value target*)]
    (cond
     (zero? (hp 0))
     []

     (armor-saves? source* target*)
     [[:entity/string-effect target "[WHITE]ARMOR"]]

     :else
     (let [min-max (:damage/min-max
                    (damage/modified source* target* damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount) 0)]
       (audiovisual/create (:position target*) :audiovisuals/damage)
       [[:entity/string-effect target (str "[RED]" dmg-amount)]
        [:e/assoc-in target [:entity/hp 0] new-hp-val]
        [:entity/fsm target (if (zero? new-hp-val) :kill :alert)]]))))
