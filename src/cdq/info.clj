(ns cdq.info
  (:require [clojure.string :as str]
            [gdl.utils :as utils]))

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

(def ^:private info-fns
  '{:creature/level creature.level/info-text
    :entity/stats cdq.entity.stats/info-text
    :effects.target/convert cdq.effects.target.convert/info-text
    :effects.target/damage cdq.effects.target.damage/info-text
    :effects.target/kill cdq.effects.target.kill/info-text
    :effects.target/melee-damage cdq.effects.target.melee-damage/info-text
    :effects.target/spiderweb cdq.effects.target.spiderweb/info-text
    :effects.target/stun cdq.effects.target.stun/info-text
    :effects/spawn cdq.effects.spawn/info-text
    :effects/target-all cdq.effects.target-all/info-text
    :entity/delete-after-duration cdq.entity.delete-after-duration/info-text
    :entity/faction cdq.entity.faction/info-text
    :entity/fsm cdq.entity.fsm/info-text
    :stats/modifiers cdq.entity.modifiers/info-text
    :entity/skills cdq.entity.skills/info-text
    :entity/species cdq.entity.species/info-text
    :entity/temp-modifier cdq.entity.temp-modifier/info-text
    :projectile/piercing? cdq.projectile.piercing/info-text
    :property/pretty-name cdq.property.pretty-name/info-text
    :skill/action-time cdq.skill.action-time/info-text
    :skill/action-time-modifier-key cdq.skill.action-time-modifier-key/info-text
    :skill/cooldown cdq.skill.cooldown/info-text
    :skill/cost cdq.skill.cost/info-text
    :maxrange cdq.projectile.maxrange/info-text})

(alter-var-root #'info-fns update-vals (fn [sym]
                                         (let [avar (requiring-resolve sym)]
                                           (assert avar sym)
                                           avar)))

(defn info-text [entity world]
  (let [component-info (fn [[k v]]
                         (let [s (if-let [info-fn (info-fns k)]
                                   (info-fn [k v] world))]
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
