(ns moon.properties.skills
  (:require [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.property :as property]))

(property/def :properties/skills
  {:overview {:title "Skills"
              :columns 16
              :image/scale 2}})

(defc :skill/action-time-modifier-key
  (component/info [[_ v]]
    (str "[VIOLET]" (case v
                      :stats/cast-speed "Spell"
                      :stats/attack-speed "Attack") "[]")))

(defc :skill/action-time
  (component/info [[_ v]]
    (str "[GOLD]Action-Time: " (readable-number v) " seconds[]")))

(defc :skill/cooldown
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[SKY]Cooldown: " (readable-number v) " seconds[]"))))

(defc :skill/cost
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[CYAN]Cost: " v " Mana[]"))))
