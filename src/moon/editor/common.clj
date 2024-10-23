(ns moon.editor.common
  (:require [moon.schema :as schema]
            [moon.utils :refer [index-of]]))

(defn widget-type [schema _]
  (let [stype (schema/type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{number? nat-int? int? pos? pos-int? :s/val-max} stype)
     number?

     :else stype)))

(def ^:private property-k-sort-order
  [:property/id
   :property/pretty-name
   :app/lwjgl3
   :entity/image
   :entity/animation
   :creature/species
   :creature/level
   :entity/body
   :item/slot
   :projectile/speed
   :projectile/max-range
   :projectile/piercing?
   :skill/action-time-modifier-key
   :skill/action-time
   :skill/start-action-sound
   :skill/cost
   :skill/cooldown])

(defn component-order [[k _v]]
  (or (index-of k property-k-sort-order) 99))
