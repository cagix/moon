(ns cdq.ctx.info-text
  (:require [cdq.modifiers :as modifiers]
            [cdq.op :as op]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [clojure.math :as math]
            [clojure.string :as str]))

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
                        :creature/stats
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

(defmulti ^:private info-segment (fn [[k] _ctx]
                                   k))

(defmethod info-segment :default [_ _ctx])

(defn info-text
  "Creates a formatted informational text representation of components."
  [ctx components]
  (->> components
       (utils/sort-by-k-order k-order)
       (keep (fn [{k 0 v 1 :as component}]
               (str (let [s (try (info-segment component ctx)
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
                      (str "\n" (info-text ctx v))))))
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

(defmethod info-segment :property/pretty-name [[_ v] _ctx]
  v)

(defmethod info-segment :maxrange [[_ v] _ctx]
  v)

(defmethod info-segment :creature/level [[_ v] _ctx]
  (str "Level: " v))

(defmethod info-segment :projectile/piercing?  [_ _ctx] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key [[_ v] _ctx]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time [[_ v] _ctx]
  (str "Action-Time: " (utils/readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cooldown: " (utils/readable-number v) " seconds")))

(defmethod info-segment :skill/cost [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(def ^:private non-val-max-stat-ks
  [:entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(defmethod info-segment :creature/stats [[k stats] _ctx]
  (str/join "\n" (concat
                  ["*STATS*"
                   (str "Mana: " (if (:entity/mana stats)
                                   (modifiers/get-mana stats)
                                   "-"))
                   (str "Hitpoints: " (modifiers/get-hitpoints stats))]
                  (for [stat-k non-val-max-stat-ks]
                    (str (str/capitalize (name stat-k)) ": "
                         (modifiers/get-stat-value stats stat-k))))))

(defmethod info-segment :effects/spawn [[_ {:keys [property/pretty-name]}] _ctx]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert [_ _ctx]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage [[_ damage] _ctx]
  (damage-info damage)
  #_(if source
      (let [modified (modifiers/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp [[k ops] _ctx]
  (op-info ops k))

(defmethod info-segment :effects.target/kill [_ _ctx]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage [_ _ctx]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb [_ _ctx]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun [[_ duration] _ctx]
  (str "Stuns for " (utils/readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all [_ _ctx]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration [[_ counter] {:keys [ctx/world]}]
  (str "Remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))

(defmethod info-segment :entity/faction [[_ faction] _ctx]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm [[_ fsm] _ctx]
  (str "State: " (name (:state fsm))))

; still used by item .. o.o
(defmethod info-segment :entity/modifiers [[_ mods] _ctx]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species [[_ species] _ctx]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier [[_ {:keys [counter]}] {:keys [ctx/world]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))

; recursively printing all effects ... thaths why deactivated ...
; custom createure info foobaz?
(defmethod info-segment :entity/skills [[_ skills] _ctx]
  ; => recursive info-text leads to endless text wall
  (when (seq skills)
    (str "Skills: " (str/join "," (map name (keys skills))))))
