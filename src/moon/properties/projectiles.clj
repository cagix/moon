(ns moon.properties.projectiles
  (:require [moon.component :as component]
            [moon.property :as property]))

(property/def :properties/projectiles
  {:schema [:entity/image
            :projectile/max-range
            :projectile/speed
            :projectile/piercing?
            :entity-effects]
   :overview {:title "Projectiles"
              :columns 16
              :image/scale 2}})

; TODO speed is 10 tiles/s but I checked moves 8 tiles/sec ... after delta time change ?

; -> range needs to be smaller than potential field range (otherwise hitting someone who can't get back at you)
; -> first range check then ray ! otherwise somewhere in contentfield out of sight
(defc :projectile/max-range {:schema pos-int?})
(defc :projectile/speed     {:schema pos-int?})

(defc :projectile/piercing? {:schema :boolean}
  (component/info [_]
    "[LIME]Piercing[]"))
