(ns moon.effect.entity.damage
  (:require [gdl.rand :refer [rand-int-between]]
            [moon.effect :refer [source target]]
            [moon.entity :as entity]
            [moon.entity.modifiers :as modifiers]))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :stats/armor-save) 0)
          (or (entity/stat source* :stats/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:stats/armor-pierce 0.4}
       target* {:stats/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defn- ->effective-damage [damage source*]
  (update damage :damage/min-max #(modifiers/effective-value source* :modifier/damage-deal %)))

(comment
 (let [->source (fn [mods] {:entity/modifiers mods})]
   (and
    (= (->effective-damage {:damage/min-max [5 10]}
                           (->source {:modifier/damage-deal {:op/val-inc [1 5 10]
                                                             :op/val-mult [0.2 0.3]
                                                             :op/max-mult [1]}}))
       {:damage/min-max [31 62]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->source {:modifier/damage-deal {:op/val-inc [1]}}))
       {:damage/min-max [6 10]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->source {:modifier/damage-deal {:op/max-mult [2]}}))
       {:damage/min-max [5 30]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->source nil))
       {:damage/min-max [5 10]}))))

(defn- damage->text [{[min-dmg max-dmg] :damage/min-max}]
  (str min-dmg "-" max-dmg " damage"))

(defn info [[_ damage]]
  (if source
    (let [modified (->effective-damage damage @source)]
      (if (= damage modified)
        (damage->text damage)
        (str (damage->text damage) "\nModified: " (damage->text modified))))
    (damage->text damage))) ; property menu no source,modifiers

(defn applicable? [_]
  (and target
       (entity/stat @target :stats/hp)))

(defn handle [[_ damage]]
  (let [source* @source
        target* @target
        hp (entity/stat target* :stats/hp)]
    (cond
     (zero? (hp 0))
     []

     (armor-saves? source* target*)
     [[:entity/string-effect target "[WHITE]ARMOR"]] ; TODO !_!_!_!_!_!

     :else
     (let [;_ (println "Source unmodified damage:" damage)
           {:keys [damage/min-max]} (->effective-damage damage source*)
           ;_ (println "\nSource modified: min-max:" min-max)
           min-max (modifiers/effective-value target* :modifier/damage-receive min-max)
           ;_ (println "effective min-max: " min-max)
           dmg-amount (rand-int-between min-max)
           ;_ (println "dmg-amount: " dmg-amount)
           new-hp-val (max (- (hp 0) dmg-amount) 0)]
       [[:tx/audiovisual (:position target*) :audiovisuals/damage]
        [:entity/string-effect target (str "[RED]" dmg-amount)]
        [:e/assoc-in target [:stats/hp 0] new-hp-val]
        [:entity/fsm target (if (zero? new-hp-val) :kill :alert)]]))))
