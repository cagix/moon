(ns cdq.effect.target
  (:require [anvil.entity :as entity]
            [cdq.context :as world :refer [add-text-effect timer]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]
            [gdl.context :as c]
            [gdl.rand :refer [rand-int-between]]))

(defcomponent :effects.target/audiovisual
  (component/applicable? [_ {:keys [effect/target]}]
    target)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ audiovisual] {:keys [effect/target]} c]
    (world/audiovisual c
                       (:position @target)
                       audiovisual)))

(defcomponent :effects.target/convert
  (component/info [_ _c]
    "Converts target to your side.")

  (component/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_ {:keys [effect/source effect/target]} c]
    (swap! target assoc :entity/faction (:entity/faction @source))))

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

(defcomponent :effects.target/damage
  (component/info [[_ damage] _c]
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

  (component/handle [[_ damage] {:keys [effect/source effect/target]} c]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target add-text-effect c "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (world/audiovisual c
                            (:position target*)
                            (c/build c :audiovisuals/damage))
         (entity/event c target (if (zero? new-hp-val) :kill :alert))
         (swap! target add-text-effect c (str "[RED]" dmg-amount "[]")))))))


#_(defn- stat-k [effect-k]
    (keyword "stats" (name effect-k)))

#_(defn info [[k ops]]
    (ops/info ops k))

#_(defn applicable? [[k _] {:keys [effect/source effect/target]}]
    (and effect/target
         (mods/value @target (stat-k k))))

#_(defn useful? [_ _ _c]
    true)

#_(defn handle [[k operations] {:keys [effect/source effect/target]}]
    (let [stat-k (stat-k k)]
      (when-let [value (mods/value @target stat-k)]
        (swap! target assoc stat-k (ops/apply operations value)))))

(defcomponent :effects.target/kill
  (component/info [_ _c]
    "Kills target")

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [_ {:keys [effect/target]} c]
    (entity/event c target :kill)))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  ; FIXME no source
  ; => to entity move
  (component/info [_ _c]
    (str "Damage based on entity strength."
         #_(when source
             (str "\n" (damage-info (entity->melee-damage @source))))))

  (component/applicable? [_ {:keys [effect/source] :as ctx}]
    (component/applicable? (damage-effect @source) ctx))

  (component/handle [_ {:keys [effect/source] :as ctx} c]
    (component/handle (damage-effect @source) ctx c)))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defcomponent :effects.target/spiderweb
    (component/info [_ _c]
      "Spiderweb slows 50% for 5 seconds."
      ; modifiers same like item/modifiers has info-text
      ; counter ?
      )

    (component/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (component/handle [_ {:keys [effect/target]} c]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer c duration)})
        (swap! target entity/mod-add modifiers)))))

(defcomponent :effects.target/stun
  (component/info [duration _c]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (component/handle [[_ duration] {:keys [effect/target]} c]
    (entity/event c target :stun duration)))
