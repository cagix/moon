(ns cdq.effects.target.damage
  (:require [cdq.entity.stats :as stats]
            [clojure.rand :refer [rand-int-between]]))

; not in stats because projectile as source doesnt have stats
; FIXME I don't see it triggering with 10 armor save ... !
(defn- effective-armor-save [source-stats target-stats]
  (max (- (or (stats/get-stat-value source-stats :stats/armor-save)   0)
          (or (stats/get-stat-value target-stats :stats/armor-pierce) 0))
       0))

(comment

 (effective-armor-save {} {:stats/modifiers {:modifiers/armor-save {:op/inc 10}}
                           :stats/armor-save 0})
 ; broken
 (let [source* {:stats/armor-pierce 0.4}
       target* {:stats/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- calc-damage
  ([source target damage]
   (update (calc-damage source damage)
           :damage/min-max
           cdq.entity.stats/apply-max
           (:stats/modifiers target)
           :modifier/damage-receive-max))
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (cdq.entity.stats/apply-min (:stats/modifiers source) :modifier/damage-deal-min)
                (cdq.entity.stats/apply-max (:stats/modifiers source) :modifier/damage-deal-max)))))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       #_(:stats/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

(defn handle [[_ damage]
              {:keys [effect/source effect/target]}
              _world]
  (let [source* @source
        target* @target
        hp (stats/get-hitpoints (:entity/stats target*))]
    (cond
     (zero? (hp 0))
     nil

     ; TODO find a better way
     (not (:entity/stats target*))
     nil

     (and (:entity/stats source*)
          (:entity/stats target*)
          (< (rand) (effective-armor-save (:entity/stats source*)
                                          (:entity/stats target*))))
     [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

     :else
     (let [min-max (:damage/min-max (calc-damage (:entity/stats source*)
                                                 (:entity/stats target*)
                                                 damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount)
                           0)]
       [[:tx/assoc-in target [:entity/stats :stats/hp 0] new-hp-val]
        [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
        [:tx/audiovisual (:body/position (:entity/body target*)) :audiovisuals/damage]
        [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]]))))

(defn info-text [[_ {[min max] :damage/min-max}] _world]
  (str min "-" max " damage")
  #_(if source
      (let [modified (stats/damage @source damage)]
        (if (= damage modified)
          (damage/info-text damage)
          (str (damage/info-text damage) "\nModified: " (damage/info modified))))
      (damage/info-text damage)) ; property menu no source,modifiers
  )
