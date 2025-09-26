(ns cdq.effects.target.damage
  (:require [cdq.entity :as entity]
            [cdq.entity.stats]
            [cdq.stats :as stats]
            [clojure.rand :refer [rand-int-between]]))

; not in stats because projectile as source doesnt have stats
; FIXME I don't see it triggering with 10 armor save ... !
(defn- effective-armor-save [source-stats target-stats]
  (max (- (or (stats/get-stat-value source-stats :entity/armor-save)   0)
          (or (stats/get-stat-value target-stats :entity/armor-pierce) 0))
       0))

(comment

 (effective-armor-save {} {:entity/modifiers {:modifiers/armor-save {:op/inc 10}}
                           :entity/armor-save 0})
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- calc-damage
  ([source target damage]
   (update (calc-damage source damage)
           :damage/min-max
           cdq.entity.stats/apply-max
           (:entity/modifiers target)
           :modifier/damage-receive-max))
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (cdq.entity.stats/apply-min (:entity/modifiers source) :modifier/damage-deal-min)
                (cdq.entity.stats/apply-max (:entity/modifiers source) :modifier/damage-deal-max)))))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

(defn handle [[_ damage]
              {:keys [effect/source effect/target]}
              _world]
  (let [source* @source
        target* @target
        hp (stats/get-hitpoints (:creature/stats target*))]
    (cond
     (zero? (hp 0))
     nil

     ; TODO find a better way
     (not (:creature/stats target*))
     nil

     (and (:creature/stats source*)
          (:creature/stats target*)
          (< (rand) (effective-armor-save (:creature/stats source*)
                                          (:creature/stats target*))))
     [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

     :else
     (let [min-max (:damage/min-max (calc-damage (:creature/stats source*)
                                                 (:creature/stats target*)
                                                 damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount)
                           0)]
       [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
        [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
        [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
        [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]]))))
