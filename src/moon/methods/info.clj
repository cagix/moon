(ns moon.methods.info
  (:require [clojure.string :as str]
            [forge.graphics.color :as color]
            [forge.utils :refer [k->pretty-name readable-number]]
            [moon.entity :as entity]
            [forge.info :as info :refer [info]]
            [moon.operations :as ops]
            [moon.world :refer [finished-ratio]]))

; TODO ops/info also here move
; value-text/info/etc.
; +?

(color/put "PRETTY_NAME" [0.84 0.8 0.52])

(.bindRoot #'forge.info/info-color
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

(.bindRoot #'forge.info/info-text-k-order
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

(defmethod info :effects.target/convert [_]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info :effects.target/damage [[_ damage]]
  (damage-info damage)
  #_(if source
      (let [modified (entity/damage-mods @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info :effects.target/kill [_]
  "Kills target")

; FIXME no source
(defmethod info :effects.target/melee-damage [_]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))
; => to entity move

(defmethod info :effects.target/spiderweb [_]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info :effects.target/stun [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info :effects/target-all [_]
  "All visible targets")

(defmethod info :entity/delete-after-duration [counter]
  (str "Remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod info :entity/faction [faction]
  (str "Faction: " (name faction)))

(defmethod info :entity/fsm [[_ fsm]]
  (str "State: " (name (:state fsm))))

(defmethod info :entity/hp [_]
  (str "Hitpoints: " (entity/hitpoints info/*entity*)))

(defmethod info :entity/mana [_]
  (str "Mana: " (entity/mana info/*entity*)))

(defmethod info :entity/modifiers [[_ mods]]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (ops/info ops k)) mods))))

#_(defmethod info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmethod info :entity/species [[_ species]]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info :entity/temp-modifier [[_ {:keys [counter]}]]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1"))

(defmethod info :property/pretty-name [[_ v]] v)
(defmethod info :maxrange             [[_ v]] v)

(defmethod info :creature/level [[_ v]]
  (str "Level: " v))

(defmethod info :projectile/piercing? [_] ; TODO also when false ?!
  "Piercing")

(defmethod info :skill/action-time-modifier-key [[_ v]]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info :skill/action-time [[_ v]]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info ::stat [[k _]]
  (str (k->pretty-name k) ": " (entity/stat info/*entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
