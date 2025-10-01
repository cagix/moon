(ns cdq.info
  (:require [cdq.entity.stats :as stats]
            [cdq.stats.ops :as ops]
            [cdq.timer :as timer]
            [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :as utils]))

(defn- ops-info-text [ops modifier-k]
  (str/join "\n"
            (keep
             (fn [[k v]]
               (when-not (zero? v)
                 (str (case (math/signum v)
                        0.0 ""
                        1.0 "+"
                        -1.0 "")
                      (case k
                        :op/inc  (str v)
                        :op/mult (str v "%"))
                      " "
                      (str/capitalize (name modifier-k)))))
             (ops/sort ops))))

(comment
 (deftest info-texts
   (is (= (ops/info-text {:op/inc -4
                          :op/mult 24}
                         "Strength")
          "-4 Strength\n+24% Strength"))

   (is (= (ops/info-text {:op/inc -4
                          :op/mult 0}
                         "Strength")
          "-4 Strength"))

   (is (= (ops/info-text {:op/mult 35}
                         "Hitpoints")
          "+35% Hitpoints"))

   (is (= (ops/info-text {:op/inc -30
                          :op/mult 5}
                         "Hitpoints")
          "-30 Hitpoints\n+5% Hitpoints")))
 )

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(def ^:private k->colors
  {:property/pretty-name "PRETTY_NAME"
   :stats/modifiers "CYAN"
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

(def ^:private k-order
  [:property/pretty-name
   :skill/action-time-modifier-key
   :skill/action-time
   :skill/cooldown
   :skill/cost
   :skill/effects
   :entity/species
   :creature/level
   :entity/stats
   :entity/delete-after-duration
   :projectile/piercing?
   :entity/projectile-collision
   :maxrange
   :entity-effects])

(def ^:private non-val-max-stat-ks
  [:stats/movement-speed
   :stats/aggro-range
   :stats/reaction-time
   :stats/strength
   :stats/cast-speed
   :stats/attack-speed
   :stats/armor-save
   :stats/armor-pierce])

(def ^:private info-fns
  {:creature/level (fn [[_ v] _world]
                     (str "Level: " v))
   :entity/stats (fn [[_ stats] _world]
                   (str/join "\n" (concat
                                   ["*STATS*"
                                    (str "Mana: " (if (:stats/mana stats)
                                                    (stats/get-mana stats)
                                                    "-"))
                                    (str "Hitpoints: " (stats/get-hitpoints stats))]
                                   (for [stat-k non-val-max-stat-ks]
                                     (str (str/capitalize (name stat-k)) ": "
                                          (stats/get-stat-value stats stat-k))))))
   :effects.target/convert (fn [_ _world]
                             "Converts target to your side.")
   :effects.target/damage (fn [[_ {[min max] :damage/min-max}] _world]
                            (str min "-" max " damage")
                            #_(if source
                                (let [modified (stats/damage @source damage)]
                                  (if (= damage modified)
                                    (damage/info-text damage)
                                    (str (damage/info-text damage) "\nModified: " (damage/info modified))))
                                (damage/info-text damage)) ; property menu no source,modifiers
                            )
   :effects.target/kill (fn [_ _world]
                          "Kills target")
   :effects.target/melee-damage (fn [_ _world]
                                  (str "Damage based on entity strength."
                                       #_(when source
                                           (str "\n" (damage-info (entity->melee-damage @source))))))
   :effects.target/spiderweb (fn [_ _world]
                               "Spiderweb slows 50% for 5 seconds.")
   :effects.target/stun (fn [[_ duration] _world]
                          (str "Stuns for " (utils/readable-number duration) " seconds"))
   :effects/spawn (fn [[_ {:keys [property/pretty-name]}] _world]
                    (str "Spawns a " pretty-name))
   :effects/target-all (fn [_ _world]
                         "All visible targets")
   :entity/delete-after-duration (fn [[_ counter] {:keys [world/elapsed-time]}]
                                   (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
   :entity/faction (fn [[_ faction] _world]
                     (str "Faction: " (name faction)))
   :entity/fsm (fn [[_ fsm] _world]
                 (str "State: " (name (:state fsm))))
   :stats/modifiers (fn [[_ mods] _world]
                      (when (seq mods)
                        (str/join "\n" (keep (fn [[k ops]]
                                               (ops-info-text ops k)) mods))))
   :entity/skills (fn [[_ skills] _world]
                    ; => recursive info-text leads to endless text wall
                    (when (seq skills)
                      (str "Skills: " (str/join "," (map name (keys skills))))))
   :entity/species (fn [[_ species] _world]
                     (str "Creature - " (str/capitalize (name species))))
   :entity/temp-modifier (fn [[_ {:keys [counter]}] {:keys [world/elapsed-time]}]
                           (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
   :projectile/piercing? (fn [_ _world]
                           "Piercing")
   :property/pretty-name (fn [[_ v] _world]
                           v)
   :skill/cooling-down? (fn [[_ counter] {:keys [world/elapsed-time]}]
                          (str "Cooldown: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))

   :skill/action-time (fn [[_ v] _world]
                        (str "Action-Time: " (utils/readable-number v) " seconds"))
   :skill/action-time-modifier-key (fn [[_ v] _world]
                                     (case v
                                       :stats/cast-speed "Spell"
                                       :stats/attack-speed "Attack"))
   :skill/cooldown (fn [[_ v] _world]
                     (when-not (zero? v)
                       (str "Cooldown: " (utils/readable-number v) " seconds")))
   :skill/cost (fn [[_ v] _world]
                 (when-not (zero? v)
                   (str "Cost: " v " Mana")))
   :maxrange (fn [[_ v] _world]
               (str "Range: " v " Meters."))})

(comment
 (:skills/death-ray (:entity/skills @(:world/player-eid (:ctx/world @cdq.application/state))))
 ; cooling-down? is not set in the action-bar ....
 ; so not showing as ui not updated
 )

(defn info-text [entity world]
  (let [component-info (fn [[k v]]
                         (let [s (if-let [info-fn (info-fns k)]
                                   (do
                                    (info-fn [k v] world)
                                    #_(str k " - " (info-fn [k v] world))))]
                           (if-let [color (k->colors k)]
                             (str "[" color "]" s "[]")
                             s)))]
    (->> entity
         (utils/sort-by-k-order k-order)
         (keep (fn [{k 0 v 1 :as component}]
                 (str (try (component-info component)
                           (catch Throwable t
                             (str "*info-error* " k)))
                      (when (map? v)
                        (str "\n" (info-text v world))))))
         (str/join "\n")
         remove-newlines)))
