(ns moon.info
  (:require [clojure.string :as str]
            [gdl.utils :refer [index-of]]
            [moon.systems.component :as component]))

(def ^:private info-text-k-order [:property/pretty-name
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

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k info-text-k-order) 99))
           components))

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(declare ^:dynamic *entity*)

(defn text [components]
  (->> components
       sort-k-order
       (keep (fn [{v 1 :as component}]
               (str (try (binding [*entity* components]
                           (component/info component))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (text v))))))
       (str/join "\n")
       remove-newlines))
