(ns gdl.info
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str]
            [gdl.utils :refer [defsystem sort-by-k-order remove-newlines]]))

(gdx/def-color "PRETTY_NAME" (gdx/color 0.84 0.8 0.52))

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

(defn- apply-color [k->colors k info-text]
  (if-let [color (k->colors k)]
    (str "[" color "]" info-text "[]")
    info-text))

(declare ^:dynamic *info-text-entity*)

(defsystem info)
(defmethod info :default [_ _context])

(defn text [c components]
  (->> components
       (sort-by-k-order k-order)
       (keep (fn [{k 0 v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (apply-color k->colors k (info component c)))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create!
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (text c v))))))
       (str/join "\n")
       remove-newlines))
