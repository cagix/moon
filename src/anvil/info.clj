(ns anvil.info
  (:require [anvil.component :as component]
            [clojure.string :as str]
            [gdl.graphics :as g]))

(g/add-color "PRETTY_NAME" [0.84 0.8 0.52])

(def k->colors {:property/pretty-name "PRETTY_NAME"
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

(def k-order [:property/pretty-name
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

(defn- apply-color [k info-text]
  (if-let [color (k->colors k)]
    (str "[" color "]" info-text "[]")
    info-text))

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k k-order) 99))
           components))

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(declare ^:dynamic *info-text-entity*)

(defn text [components]
  (->> components
       sort-k-order
       (keep (fn [{k 0 v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (apply-color k (component/info component)))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (text v))))))
       (str/join "\n")
       remove-newlines))

(defn k->pretty-name [k]
  (str/capitalize (name k)))
