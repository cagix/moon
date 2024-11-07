(ns moon.stats
  (:require [gdl.system :refer [defmethods *k*]]
            [gdl.utils :refer [mapvals k->pretty-name]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.entity.modifiers :as mods]
            [moon.entity.mana :as mana]
            [moon.effect :as effect]
            [moon.operations :as ops]))

(defmethod component/info :entity/stat [[k v]]
  (str (k->pretty-name k) ": " (mods/value component/*info-text-entity* k)))

(defn defstat [k]
  {:pre [(= (namespace k) "stats")]}
  (derive k :entity/stat))

; TODO negate this value also @ use (modifier damage receive)
; so can make positiive modifeirs green , negative red....

; TODO needs to be there for each npc - make non-removable (added to all creatures)
; and no need at created player (npc controller component?)
(defstat :stats/aggro-range)
(defstat :stats/reaction-time)

; * TODO clamp/post-process effective-values @ stat-k->effective-value
; * just don't create movement-speed increases too much?
; * dont remove strength <0 or floating point modifiers  (op/int-inc ?)
; * cast/attack speed dont decrease below 0 ??

; TODO clamp between 0 and max-speed ( same as movement-speed-schema )
(defstat :stats/movement-speed) ;(m/form entity/movement-speed-schema)

; TODO show the stat in different color red/green if it was permanently modified ?
; or an icon even on the creature
; also we want audiovisuals always ...

; TODO clamp into ->pos-int
(defstat :stats/strength)

; TODO here >0
(comment
 (let [doc "action-time divided by this stat when a skill is being used.
           Default value 1.

           For example:
           attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."]))
(defstat :stats/cast-speed)
(defstat :stats/attack-speed)

; TODO bounds
(defstat :stats/armor-save)
(defstat :stats/armor-pierce)
