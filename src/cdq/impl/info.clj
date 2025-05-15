(ns cdq.impl.info
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.info :refer [info-segment]]
            [cdq.op :as op]
            [cdq.timer :as timer]
            [cdq.utils :refer [readable-number]]
            [clojure.math :as math]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics Color Colors)))

(Colors/put "PRETTY_NAME" (Color. (float 0.84) (float 0.8) (float 0.52) (float 1)))

(defmulti ^:private op-value-text (fn [[k]]
                                    k))

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

(defmethod info-segment :property/pretty-name [[_ v] _entity] v)

(defmethod info-segment :maxrange [[_ v] _entity] v)

(defmethod info-segment :creature/level [[_ v] _entity] (str "Level: " v))

(defmethod info-segment :projectile/piercing?  [_ _entity] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key [[_ v] _entity]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time [[_ v] _entity]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown [[_ v] _entity]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info-segment :skill/cost [[_ v] _entity]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info-segment ::stat [[k _] entity]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod info-segment :effects/spawn [[_ {:keys [property/pretty-name]}] _entity]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert [_ _entity]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage [[_ damage] _entity]
  (damage-info damage)
  #_(if source
      (let [modified (entity/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp [[k ops] _entity]
  (op-info ops k))

(defmethod info-segment :effects.target/kill [_ _entity]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage [_ _entity]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb [_ _entity]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun [[_ duration] _entity]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all [_ _entity]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration [[_ counter] _entity]
  (str "Remaining: " (readable-number (timer/ratio ctx/elapsed-time counter)) "/1"))

(defmethod info-segment :entity/faction [[_ faction] _entity]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm [[_ fsm] _entity]
  (str "State: " (name (:state fsm))))

(defmethod info-segment :entity/hp [_ entity]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defmethod info-segment :entity/mana [_ entity]
  (str "Mana: " (entity/mana entity)))

(defmethod info-segment :entity/modifiers [[_ mods] _entity]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species [[_ species] _entity]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier [[_ {:keys [counter]}] _entity]
  (str "Spiderweb - remaining: " (readable-number (timer/ratio ctx/elapsed-time counter)) "/1"))

#_(defmethod info-segment :entity/skills [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))
