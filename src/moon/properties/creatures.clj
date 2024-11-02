(ns moon.properties.creatures
  (:require [clojure.string :as str]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.property :as property]))

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from entity
(property/def :properties/creatures
  {:overview {:title "Creatures"
              :columns 15
              :image/scale 1.5
              :sort-by-fn #(vector (:creature/level %)
                                   (name (:creature/species %))
                                   (name (:property/id %)))
              :extra-info-text #(str (:creature/level %))}})

(defc :property/pretty-name
  {:let value}
  (component/info [_]
    (str "[PRETTY_NAME]"value"[]")))

(defc :creature/species
  (entity/->v [[_ species]]
    (str/capitalize (name species)))
  (component/info [[_ species]]
    (str "[LIGHT_GRAY]Creature - " species "[]")))

(defc :creature/level
  (component/info [[_ lvl]]
    (str "[GRAY]Level " lvl "[]")))
