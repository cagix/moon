(ns moon.component
  (:require [clojure.string :as str]
            [gdl.utils :refer [index-of]]
            [gdl.system :refer [defsystem]]))

(defsystem handle)

(defsystem applicable?)

(defsystem useful?)
(defmethod useful? :default [_] true)

(defsystem render)
(defmethod render :default [_])

(defsystem info)
(defmethod info :default [_])

(def ^:private info-text-k-order [:property/pretty-name
                                  :skill/action-time-modifier-key
                                  :skill/action-time
                                  :skill/cooldown
                                  :skill/cost
                                  :skill/effects
                                  :creature/species
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

(declare ^:dynamic *info-text-entity*)

(defn ->info [components]
  (->> components
       sort-k-order
       (keep (fn [{v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (info component))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (->info v))))))
       (str/join "\n")
       remove-newlines))
