(ns moon.properties.creatures
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.utils :refer [safe-merge]]
            [moon.component :refer [defc] :as component]
            [moon.entity :as entity]
            [moon.property :as property]))

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

(color/put "ITEM_GOLD" [0.84 0.8 0.52])

(defc :property/pretty-name
  {:schema :string
   :let value}
  (component/info [_]
    (str "[ITEM_GOLD]"value"[]")))

(defc :body/width   {:schema pos?})
(defc :body/height  {:schema pos?})
(defc :body/flying? {:schema :boolean})

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from entity

(defc :entity/body
  {:schema [:s/map [:body/width
                :body/height
                :body/flying?]]})

(defc :creature/species
  {:schema [:qualified-keyword {:namespace :species}]}
  (entity/->v [[_ species]]
    (str/capitalize (name species)))
  (component/info [[_ species]]
    (str "[LIGHT_GRAY]Creature - " species "[]")))

(defc :creature/level
  {:schema pos-int?}
  (component/info [[_ lvl]]
    (str "[GRAY]Level " lvl "[]")))
