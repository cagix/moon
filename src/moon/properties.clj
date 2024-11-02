(ns moon.properties
  (:require [clojure.string :as str]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.item :as item]
            [moon.modifiers :as mods]))

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from entity

(defmethods :property/pretty-name
  {:let value}
  (component/info [_]
    (str "[PRETTY_NAME]"value"[]")))

(defmethods :creature/species
  (entity/->v [[_ species]]
    (str/capitalize (name species)))
  (component/info [[_ species]]
    (str "[LIGHT_GRAY]Creature - " species "[]")))

(defmethods :creature/level
  (component/info [[_ lvl]]
    (str "[GRAY]Level " lvl "[]")))

(defmethods :item/modifiers
  (component/info [[_ value-mods]]
    (str (mods/info-text value-mods)
         "\n [GRAY]"
         (binding [*print-level* nil]
           (with-out-str
            (clojure.pprint/pprint
             value-mods)))
         "[]")))

; TODO speed is 10 tiles/s but I checked moves 8 tiles/sec ... after delta time change ?

; -> range needs to be smaller than potential field range (otherwise hitting someone who can't get back at you)
; -> first range check then ray ! otherwise somewhere in contentfield out of sight
#_(defc :projectile/max-range {:schema pos-int?})
#_(defc :projectile/speed     {:schema pos-int?})

(defmethods :projectile/piercing?
  (component/info [_]
    "[LIME]Piercing[]"))

#_(defc :world/max-area-level {:schema pos-int?}) ; TODO <= map-size !?
#_(defc :world/spawn-rate {:schema pos?}) ; TODO <1 !

(defmethods :skill/action-time-modifier-key
  (component/info [[_ v]]
    (str "[VIOLET]" (case v
                      :stats/cast-speed "Spell"
                      :stats/attack-speed "Attack") "[]")))

(defmethods :skill/action-time
  (component/info [[_ v]]
    (str "[GOLD]Action-Time: " (readable-number v) " seconds[]")))

(defmethods :skill/cooldown
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[SKY]Cooldown: " (readable-number v) " seconds[]"))))

(defmethods :skill/cost
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[CYAN]Cost: " v " Mana[]"))))
