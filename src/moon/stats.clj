(ns moon.stats)

; TODO need to 'install' them first as components to get 'info'
; have a 'stat' itself - w. ' :base-value/:ops ' ?
; its the value?

(derive :stats/aggro-range   :entity/stat)
(derive :stats/reaction-time :entity/stat)

; * TODO clamp/post-process effective-values @ stat-k->effective-value
; * just don't create movement-speed increases too much?
; * dont remove strength <0 or floating point modifiers  (op/int-inc ?)
; * cast/attack speed dont decrease below 0 ??

; TODO clamp between 0 and max-speed ( same as movement-speed-schema )
;(m/form entity/movement-speed-schema)
(derive :stats/movement-speed :entity/stat)

; TODO show the stat in different color red/green if it was permanently modified ?
; or an icon even on the creature
; also we want audiovisuals always ...

; TODO clamp into ->pos-int
(derive :stats/strength :entity/stat)

; TODO here >0
(comment
 (let [doc "action-time divided by this stat when a skill is being used.
           Default value 1.

           For example:
           attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."]))
(derive :stats/cast-speed :entity/stat)
(derive :stats/attack-speed :entity/stat)

; TODO bounds
(derive :stats/armor-save :entity/stat)
(derive :stats/armor-pierce :entity/stat)
