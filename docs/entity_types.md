
  ; * item:
  {:entity/image , :entity/item , :entity/clickable }

  ; projectile:
  {:entity/movement
   :entity/image
   :entity/faction
   :entity/delete-after-duration
   :entity/destroy-audiovisual
   :entity/projectile-collision }

; audiovisual:
; :entity/animation
; :entity/delete-after-animation-stopped?

; alert:
; :entity/alert-friendlies-after-duration

; line
; :entity/line-render
; :entity/delete-after-duration

; * spawn-creature! -> assert there extra?
; :entity/destroy-audiovisual
; :entity/fsm ; required ! - >assert @ spawn-creature
; :entity/faction ; required ! - >assert @ spawn-creature

; player-entity-props: (with fsm,faction )
; :entity/player?
; :entity/free-skill-points
; :entity/clickable
; :entity/click-distance-tiles

; schema props !:
[:s/map [:property/id
         :property/pretty-name
         :entity/species
         :creature/level
         :entity/body ; <- is dissoc'ed
         :entity/animation

         ; all stats: !
         :entity/hp
         :entity/movement-speed
         :entity/aggro-range
         :entity/reaction-time
         :entity/skills
         [:entity/mana         {:optional true}] ; why all optional ?!
         [:entity/strength     {:optional true}]
         [:entity/cast-speed   {:optional true}]
         [:entity/attack-speed {:optional true}]
         [:entity/armor-save   {:optional true}]
         [:entity/armor-pierce {:optional true}]
         [:entity/modifiers    {:optional true}]
         [:entity/inventory    {:optional true}] ; only player? required? cant play other creature types
         [:entity/color        {:optional true}]]] ; unused

