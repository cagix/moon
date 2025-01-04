(ns cdq.info
  (:require [cdq.context :refer [finished-ratio]]
            [cdq.operation :as op]
            [cdq.entity :as entity]
            [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [defsystem readable-number]]
            [gdl.info :as info]))

(defsystem op-value-text)

(defmethod op-value-text :op/inc
  [[_ value]]
  (str value))

(defmethod op-value-text :op/mult
  [[_ value]]
  (str value "%"))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn- op-info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text component) " " (str/capitalize (name k)))))
             (sort-by op/-order op))))

(defmethod info/text :property/pretty-name
  [[_ v] _entity _c]
  v)

(defmethod info/text :maxrange
  [[_ v] _entity _c]
  v)

(defmethod info/text :creature/level
  [[_ v] _entity _c]
  (str "Level: " v))

(defmethod info/text :projectile/piercing?
  [_ _entity _c] ; TODO also when false ?!
  "Piercing")

(defmethod info/text :skill/action-time-modifier-key
  [[_ v] _entity _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info/text :skill/action-time
  [[_ v] _entity _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info/text :skill/cooldown
  [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info/text :skill/cost
  [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info/text ::stat
  [[k _] entity _c]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod info/text :effects/spawn
  [[_ {:keys [property/pretty-name]}] _entity _context]
  (str "Spawns a " pretty-name))

(defmethod info/text :effects.target/convert
  [_ _entity _c]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info/text :effects.target/damage
  [[_ damage] _entity _c]
  (damage-info damage)
  #_(if source
      (let [modified (entity/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info/text :effects.target/hp
  [[k ops] _entity _context]
  (op-info ops k))

(defmethod info/text :effects.target/kill
  [_ _entity _c]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info/text :effects.target/melee-damage
  [_ _entity _c]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info/text :effects.target/spiderweb
  [_ _entity _c]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info/text :effects.target/stun
  [[_ duration] _entity _c]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info/text :effects/target-all
  [_ _entity _c]
  "All visible targets")

(defmethod info/text :entity/delete-after-duration
  [[_ counter] _entity c]
  (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

(defmethod info/text :entity/faction
  [[_ faction] _entity _c]
  (str "Faction: " (name faction)))

(defmethod info/text :entity/fsm
  [[_ fsm] _entity _c]
  (str "State: " (name (:state fsm))))

(defmethod info/text :entity/hp
  [_ entity _c]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defmethod info/text :entity/mana
  [_ entity _c]
  (str "Mana: " (entity/mana entity)))

(defmethod info/text :entity/modifiers
  [[_ mods] _entity _c]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info/text :entity/species
  [[_ species] _entity _c]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info/text :entity/temp-modifier
  [[_ {:keys [counter]}] _entity c]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio c counter)) "/1"))
