(ns cdq.tx.deal-damage
  (:require [cdq.world.entity :as entity]
            [cdq.world.entity.stats :as modifiers]
            [cdq.rand :refer [rand-int-between]]))

(defn do!
  [[_ source target damage] _ctx]
  (let [source* @source
        target* @target
        hp (modifiers/get-hitpoints (:creature/stats target*))]
    (cond
     (zero? (hp 0))
     nil

     (< (rand) (modifiers/effective-armor-save (:creature/stats source*)
                                               (:creature/stats target*)))
     [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

     :else
     (let [min-max (:damage/min-max (modifiers/damage (:creature/stats source*)
                                                      (:creature/stats target*)
                                                      damage))
           dmg-amount (rand-int-between min-max)
           new-hp-val (max (- (hp 0) dmg-amount)
                           0)]
       [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
        [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
        [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
        [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]]))))
