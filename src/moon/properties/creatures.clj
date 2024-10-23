(ns moon.properties.creatures
  (:require [moon.property :as property]))

(property/def :properties/creatures
  {:schema [:entity/body
            :property/pretty-name
            :creature/species
            :creature/level
            :entity/animation
            :stats/hp
            :stats/movement-speed
            :stats/aggro-range
            :stats/reaction-time
            [:stats/mana          {:optional true}]
            [:stats/strength      {:optional true}]
            [:stats/cast-speed    {:optional true}]
            [:stats/attack-speed  {:optional true}]
            [:stats/armor-save    {:optional true}]
            [:stats/armor-pierce  {:optional true}]
            :entity/skills
            [:entity/modifiers {:optional true}]
            [:entity/inventory {:optional true}]]
   :overview {:title "Creatures"
              :columns 15
              :image/scale 1.5
              :sort-by-fn #(vector (:creature/level %)
                                   (name (:creature/species %))
                                   (name (:property/id %)))
              :extra-info-text #(str (:creature/level %))}})
