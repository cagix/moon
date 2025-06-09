
(ns cdq.ctx)

; TODO
; * OBSERVABILITY proper tree/table view for entities ?!
; dev menu extend? debug flags?

; TODO don't pass context around, use it directly on the object ?
; e.g. sprites?

; TODO all global vars / configs
; => e.g. info k->colors or editor or max-delta etc
; move into 'config' => APP VALUES TREE

(declare ^{:doc "Implements [[cdq.assets/Assets]] and `clojure.lang.IFn` with one arg, like this: `(ctx/assets \"my_image.png\")`.

                Returns the asset or throws an exception if it cannot be found."}
         assets
         ; contains Textures/Sounds from `.bmp`, `'wav', `png' in 'resources/'

         ^{:doc "[[cdq.graphics/Graphics]]."}
         graphics
         #_(comment
          [^Batch batch
           ^Texture shape-drawer-texture
           shape-drawer
           cursors
           default-font
           world-unit-scale
           world-viewport ; -> world-camera we change zoom ... mutable ...
           get-tiled-map-renderer
           unit-scale
           ui-viewport]
          )

         stage
         ; * (editor ?)
         ; * dev-menu
         ; * hp-mana-bar
         ; * ... ?


         ^{:doc "[[cdq.db/DB]]"}
         db
         ; * audiovisuals
         ; * creatures
         ; * items
         ; * projectiles
         ; * skills

         ^{:doc "[[cdq.world/World]]."}
         world
         #_(comment
          [tiled-map
           grid ; cell:
           (comment
            [position
             middle ; only used @ potential-field-follow-to-enemy -> can remove it.
             adjacent-cells
             movement
             entities
             occupied
             good
             evil])
           raycaster
           content-grid
           explored-tile-corners
           entity-ids
           potential-field-cache
           active-entities])

         elapsed-time
         delta-time
         player-eid
         paused?)

(def mouseover-eid nil)

(comment
 (clojure.pprint/pprint
  (sort (keys @mouseover-eid)))
 )

(comment
 ; What is actually an entity schema now ???

 ; * spawn-entity
 ; spawn-audiovisual ; effect-body (non colliding, interactable)
 ; spawn-creature
 ; spawn-item
 ; delayed-alert ; effect-body (non colliding, interactable)
 ; line-render ; effect-body (non colliding, interactable)
 ; spawn-projectile ; -> interactable ??

   (comment
    ; Body:
    [position
     left-bottom
     width
     height
     half-width
     half-height
     radius
     collides?
     z-order
     rotation-angle]

    ; TODO grep /entity: ( IMPLICIT SCHEMA OVER THE WHOLE APP !)
    ; * cdq.entity [ ok]
    ; * cdq.dev [ ok]
    ; * cdq.g [ ok]
    ; * cdq.impl.effects [ ok]
    ; * cdq.impl.entity
    ; * cdq.impl.info [ ok]
    ; * cdq.info [ ok]
    ; * cdq.ui.entity-info-window [ ok]
    ; -> see also 'schema.edn'

    ; Entity types ( have different schema ? )
    ; * audiovisual
    ; * item
    ; * alert
    ; * line
    ; * projectile
    ; * creature

    :property/id
    :property/pretty-name
    :entity/id
    :entity/destroyed?
    :entity/mouseover?
    :entity/clickable

    :cdq.content-grid/content-cell
    ::touched-cells
    ::occupied-cells

    :entity/animation
    :entity/delete-after-animation-stopped?
    :entity/image
    :entity/string-effect
    :entity/line-render

    :entity/item

    :entity/alert-friendlies-after-duration

    :entity/delete-after-duration

    :entity/faction

    :entity/movement

    :entity/destroy-audiovisual

    :entity/projectile-collision
    :projectile/piercing?

    :entity/fsm ; =>> all states !
    ; :active-skill
    ; :stunned

    ; :player-idle
    ; :player-moving
    ; :player-item-on-cursor
    ; :player-dead
    :entity/item-on-cursor

    ; :npc-dead
    ; :npc-idle
    ; :npc-moving
    ; :npc-sleeping

    :entity/player?
    :entity/free-skill-points ; unused
    :entity/click-distance-tiles ; game setting global (can change?)


    :entity/skills
    :entity/inventory
    :entity/species
    :creature/level

    ;; STATS !!!
    :entity/modifiers
    :entity/aggro-range
    :entity/hp
    :entity/mana
    :entity/movement-speed
    :entity/reaction-time
    :entity/strength
    :entity/cast-speed
    :entity/attack-speed
    :entity/armor-save
    :entity/armor-pierce

    :entity/temp-modifier
    )
 )
