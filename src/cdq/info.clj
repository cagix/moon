(ns cdq.info
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.op :as op]
            [cdq.utils :refer [sort-by-k-order
                               readable-number]]
            [gdl.graphics :as graphics]
            [clojure.math :as math]
            [clojure.string :as str]))

(graphics/def-markdown-color "PRETTY_NAME" (graphics/color 0.84 0.8 0.52 1))

(def ^:private k->colors {:property/pretty-name "PRETTY_NAME"
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

(def ^:private k-order [:property/pretty-name
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

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(defmulti ^:private info-segment (fn [[k] _entity ctx]
                         k))
(defmethod info-segment :default [_ _entity ctx])

(defn text
  "Creates a formatted informational text representation of components."
  [ctx components]
  (->> components
       (sort-by-k-order k-order)
       (keep (fn [{k 0 v 1 :as component}]
               (str (let [entity components
                          s (try (info-segment component entity ctx)
                                 (catch Throwable t
                                   ; fails for
                                   ; effects/spawn
                                   ; end entity/hp
                                   ; as already 'built' yet 'hp' not
                                   ; built from db yet ...
                                   (pr-str component)
                                   #_(throw (ex-info "info system failed"
                                                     {:component component}
                                                     t))))]
                      (if-let [color (k->colors k)]
                        (str "[" color "]" s "[]")
                        s))
                    (when (map? v)
                      (str "\n" (text ctx v))))))
       (str/join "\n")
       remove-newlines))

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

(defmethod info-segment :property/pretty-name [[_ v] _entity _ctx]
  v)

(defmethod info-segment :maxrange [[_ v] _entity _ctx]
  v)

(defmethod info-segment :creature/level [[_ v] _entity _ctx]
  (str "Level: " v))

(defmethod info-segment :projectile/piercing?  [_ _entity _ctx] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key [[_ v] _entity _ctx]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time [[_ v] _entity _ctx]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown [[_ v] _entity _ctx]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info-segment :skill/cost [[_ v] _entity _ctx]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info-segment ::stat [[k _] entity _ctx]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod info-segment :effects/spawn [[_ {:keys [property/pretty-name]}] _entity _ctx]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert [_ _entity _ctx]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage [[_ damage] _entity _ctx]
  (damage-info damage)
  #_(if source
      (let [modified (modifiers/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp [[k ops] _entity _ctx]
  (op-info ops k))

(defmethod info-segment :effects.target/kill [_ _entity _ctx]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage [_ _entity _ctx]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb [_ _entity _ctx]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun [[_ duration] _entity _ctx]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all [_ _entity _ctx]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration [[_ counter] _entity ctx]
  (str "Remaining: " (readable-number (g/timer-ratio ctx counter)) "/1"))

(defmethod info-segment :entity/faction [[_ faction] _entity _ctx]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm [[_ fsm] _entity _ctx]
  (str "State: " (name (:state fsm))))

(defmethod info-segment :entity/hp [_ entity _ctx]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defmethod info-segment :entity/mana [_ entity _ctx]
  (str "Mana: " (entity/mana entity)))

(defmethod info-segment :entity/modifiers [[_ mods] _entity _ctx]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species [[_ species] _entity _ctx]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier [[_ {:keys [counter]}] _entity ctx]
  (str "Spiderweb - remaining: " (readable-number (g/timer-ratio ctx counter)) "/1"))

#_(defmethod info-segment :entity/skills [skills _ctx]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))
