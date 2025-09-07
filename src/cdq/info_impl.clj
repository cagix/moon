(ns cdq.info-impl
  (:require [cdq.stats :as modifiers]
            [cdq.timer :as timer]
            [cdq.op :as op]
            [cdq.utils :as utils]
            [clojure.math :as math]
            [clojure.string :as str]))

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

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(def ^:private non-val-max-stat-ks
  [:entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(def info-fns
  {:k->colors {:property/pretty-name "PRETTY_NAME"
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
               :entity/temp-modifier "LIGHT_GRAY"}
   :k-order [:property/pretty-name
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
             :entity-effects]
   :info-fns {:creature/level (fn [[_ v] _ctx]
                                (str "Level: " v))
              :creature/stats (fn [[k stats] _ctx]
                                (str/join "\n" (concat
                                                ["*STATS*"
                                                 (str "Mana: " (if (:entity/mana stats)
                                                                 (modifiers/get-mana stats)
                                                                 "-"))
                                                 (str "Hitpoints: " (modifiers/get-hitpoints stats))]
                                                (for [stat-k non-val-max-stat-ks]
                                                  (str (str/capitalize (name stat-k)) ": "
                                                       (modifiers/get-stat-value stats stat-k))))))
              :effects.target/convert (fn [_ _ctx]
                                        "Converts target to your side.")
              :effects.target/damage (fn [[_ damage] _ctx]
                                       (damage-info damage)
                                       #_(if source
                                           (let [modified (modifiers/damage @source damage)]
                                             (if (= damage modified)
                                               (damage-info damage)
                                               (str (damage-info damage) "\nModified: " (damage/info modified))))
                                           (damage-info damage)) ; property menu no source,modifiers
                                       )
              :effects.target/kill (fn [_ _ctx] "Kills target")
              :effects.target/melee-damage (fn [_ _ctx]
                                             (str "Damage based on entity strength."
                                                  #_(when source
                                                      (str "\n" (damage-info (entity->melee-damage @source))))))
              :effects.target/spiderweb (fn [_ _ctx] "Spiderweb slows 50% for 5 seconds.")
              :effects.target/stun (fn [[_ duration] _ctx]
                                     (str "Stuns for " (utils/readable-number duration) " seconds"))
              :effects/spawn (fn [[_ {:keys [property/pretty-name]}] _ctx]
                               (str "Spawns a " pretty-name))
              :effects/target-all (fn [_ _ctx]
                                    "All visible targets")
              :entity/delete-after-duration (fn [[_ counter] {:keys [ctx/elapsed-time]}]
                                              (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
              :entity/faction (fn [[_ faction] _ctx]
                                (str "Faction: " (name faction)))
              :entity/fsm (fn [[_ fsm] _ctx]
                            (str "State: " (name (:state fsm))))
              :entity/modifiers (fn [[_ mods] _ctx]
                                  (when (seq mods)
                                    (str/join "\n" (keep (fn [[k ops]]
                                                           (op-info ops k)) mods))))
              :entity/skills (fn [[_ skills] _ctx]
                               ; => recursive info-text leads to endless text wall
                               (when (seq skills)
                                 (str "Skills: " (str/join "," (map name (keys skills))))))
              :entity/species (fn [[_ species] _ctx]
                                (str "Creature - " (str/capitalize (name species))))
              :entity/temp-modifier (fn [[_ {:keys [counter]}] {:keys [ctx/elapsed-time]}]
                                      (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
              :projectile/piercing? (fn [_ _ctx] ; TODO also when false ?!
                                      "Piercing")
              :property/pretty-name (fn [[_ v] _ctx]
                                      v)
              :skill/action-time (fn [[_ v] _ctx]
                                   (str "Action-Time: " (utils/readable-number v) " seconds"))
              :skill/action-time-modifier-key (fn [[_ v] _ctx]
                                                (case v
                                                  :entity/cast-speed "Spell"
                                                  :entity/attack-speed "Attack"))
              :skill/cooldown (fn [[_ v] _ctx]
                                (when-not (zero? v)
                                  (str "Cooldown: " (utils/readable-number v) " seconds")))
              :skill/cost (fn [[_ v] _ctx]
                            (when-not (zero? v)
                              (str "Cost: " v " Mana")))
              :maxrange (fn [[_ v] _ctx] v)}})
