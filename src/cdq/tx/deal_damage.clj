(ns cdq.tx.deal-damage
  (:require [cdq.ctx.effect-handler :refer [do! handle-txs!]]
            [cdq.entity :as entity]
            [cdq.modifiers :as modifiers]
            [cdq.rand :refer [rand-int-between]]))

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

(defmethod do! :tx/deal-damage [[_ source target damage] ctx]
  (let [source* @source
        target* @target
        hp (entity/hitpoints target*)]
    (handle-txs! ctx
                 (cond
                  (zero? (hp 0))
                  nil

                  (armor-saves? source* target*) ; TODO BUG ALWAYS ARMOR ?
                  [[:tx/add-text-effect target "[WHITE]ARMOR"]]

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
                     [:tx/add-text-effect target (str "[RED]" dmg-amount "[]")]])))))
