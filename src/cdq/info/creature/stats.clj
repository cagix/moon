(ns cdq.info.creature.stats
  (:require [cdq.stats :as modifiers]
            [clojure.string :as str]))

(def ^:private non-val-max-stat-ks
  [:entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(defn info-segment [[k stats] _ctx]
  (str/join "\n" (concat
                  ["*STATS*"
                   (str "Mana: " (if (:entity/mana stats)
                                   (modifiers/get-mana stats)
                                   "-"))
                   (str "Hitpoints: " (modifiers/get-hitpoints stats))]
                  (for [stat-k non-val-max-stat-ks]
                    (str (str/capitalize (name stat-k)) ": "
                         (modifiers/get-stat-value stats stat-k))))))
