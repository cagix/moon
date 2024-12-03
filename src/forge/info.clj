(ns forge.info
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [forge.core :refer :all]
            [forge.system :refer [defsystem]]))

(add-color "PRETTY_NAME" [0.84 0.8 0.52])

(bind-root #'info-color
           {:property/pretty-name "PRETTY_NAME"
            :entity/modifiers "CYAN"
            :maxrange "LIGHT_GRAY"
            :creature/level "GRAY"
            :projectile/piercing? "LIME"
            :skill/action-time-modifier-key "VIOLET"
            :skill/action-time "GOLD"
            :skill/cooldown "SKY"
            :skill/cost "CYAN"
            :entity/delete-after-duration "LIGHT_GRAY"
            :entity/faction "SLATE"
            :entity/fsm "YELLOW"
            :entity/species "LIGHT_GRAY"
            :entity/temp-modifier "LIGHT_GRAY"})

(bind-root #'info-text-k-order
           [:property/pretty-name
            :skill/action-time-modifier-key
            :skill/action-time
            :skill/cooldown
            :skill/cost
            :skill/effects
            :entity/species
            :creature/level
            :entity/hp
            :entity/mana
            :entity/strength
            :entity/cast-speed
            :entity/attack-speed
            :entity/armor-save
            :entity/delete-after-duration
            :projectile/piercing?
            :entity/projectile-collision
            :maxrange
            :entity-effects])

(defmethod component-info :effects.target/convert [_]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod component-info :effects.target/damage [[_ damage]]
  (damage-info damage)
  #_(if source
      (let [modified (damage-mods @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod component-info :effects.target/kill [_]
  "Kills target")

; FIXME no source
(defmethod component-info :effects.target/melee-damage [_]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))
; => to entity move

(defmethod component-info :effects.target/spiderweb [_]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod component-info :effects.target/stun [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod component-info :effects/target-all [_]
  "All visible targets")

(defmethod component-info :entity/delete-after-duration [counter]
  (str "Remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod component-info :entity/faction [faction]
  (str "Faction: " (name faction)))

(defmethod component-info :entity/fsm [[_ fsm]]
  (str "State: " (name (:state fsm))))

(defmethod component-info :entity/hp [_]
  (str "Hitpoints: " (hitpoints *info-text-entity*)))

(defmethod component-info :entity/mana [_]
  (str "Mana: " (e-mana *info-text-entity*)))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defsystem op-value-text)

(defmethod op-value-text :op/inc [[_ value]]
  (str value))

(defmethod op-value-text :op/mult [[_ value]]
  (str value "%"))

(defn- ops-info [ops k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as op}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text op) " " (k->pretty-name k))))
             (sort-by op-order ops))))

(defmethod component-info :entity/modifiers [[_ mods]]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (ops-info ops k)) mods))))

#_(defmethod component-info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmethod component-info :entity/species [[_ species]]
  (str "Creature - " (str/capitalize (name species))))

(defmethod component-info :entity/temp-modifier [[_ {:keys [counter]}]]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod component-info :property/pretty-name [[_ v]] v)
(defmethod component-info :maxrange             [[_ v]] v)

(defmethod component-info :creature/level [[_ v]]
  (str "Level: " v))

(defmethod component-info :projectile/piercing? [_] ; TODO also when false ?!
  "Piercing")

(defmethod component-info :skill/action-time-modifier-key [[_ v]]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod component-info :skill/action-time [[_ v]]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod component-info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod component-info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod component-info ::stat [[k _]]
  (str (k->pretty-name k) ": " (e-stat *info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
