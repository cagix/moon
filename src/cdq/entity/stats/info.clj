(ns cdq.entity.stats.info
  (:require [cdq.entity.stats :as stats]
            [clojure.string :as str]))

(def ^:private non-val-max-stat-ks
  [:stats/movement-speed
   :stats/aggro-range
   :stats/reaction-time
   :stats/strength
   :stats/cast-speed
   :stats/attack-speed
   :stats/armor-save
   :stats/armor-pierce])

(defn text [[_ stats] _world]
  (str/join "\n" (concat
                  ["*STATS*"
                   (str "Mana: " (stats/get-mana stats))
                   (str "Hitpoints: " (stats/get-hitpoints stats))]
                  (for [stat-k non-val-max-stat-ks]
                    (str (str/capitalize (name stat-k)) ": "
                         (stats/get-stat-value stats stat-k))))))
