(ns moon.components
  (:require (moon.effect projectile
                         spawn
                         target-all
                         target-entity)
            (moon.effect.entity convert
                                damage
                                kill
                                melee-damage
                                spiderweb
                                stun)
            (moon.entity.npc dead
                             idle
                             moving
                             sleeping)
            (moon.entity.player dead
                                idle
                                item-on-cursor
                                moving)
            (moon.entity active
                         animation
                         clickable
                         delete-after-animation-stopped
                         delete-after-duration
                         destroy-audiovisual
                         faction
                         fsm
                         hitpoints ; :stats/hp not :entity/hitpoints
                         image
                         inventory ; lots of :tx, can-pickup-item?
                         line-render
                         modifiers ; lots of stuff
                         mouseover
                         movement ; max-delta-time, speed-schema
                         player
                         projectile
                         skills ; has-skill?
                         string-effect
                         stunned
                         temp-modifier)
            (moon.fsms player
                       npc) ; public vars, just plain data and call fsm-inc in state
            (moon.level generate
                        uf-caves
                        tiled-map)
            (moon.operation inc
                            mult
                            val-max) ; omgwtf

            ; those are not defc yet
            (moon.schema animation
                         boolean
                         enum
                         image
                         map
                         number
                         one-to-many
                         one-to-one
                         sound
                         string
                         val-max)

            (moon.tx audiovisual
                     creature
                     cursor
                     effect
                     entity ; lots o stuff
                     item
                     line-render
                     projectile
                     sound)

            ; public fns
            (moon.widgets action-bar
                          dev-menu
                          entity-info-window
                          hp-mana
                          inventory
                          player-message
                          player-modal
                          properties-overview
                          properties-tabs
                          property)
            moon.colors
            moon.properties
            moon.world.widgets))

(comment

 (require '[clojure.string :as str])
 (import clojure.lang.MultiFn)

 (defn- ns->k [ns]
   (let [ns-parts (-> *ns*
                      ns-name
                      (str/replace #"^moon." "")
                      (str/split #"\."))]
     (keyword (str/join "." (drop-last ns-parts))
              (last ns-parts))))


 (seq (.listFiles (io/file "src/moon/effect/entity/")))

 (MultiFn/.addMethod component/info        :foo nil)
 (ns-resolve 'moon.effect.entity.convert 'info)

 (defn- add-method [system k avar]
   {:pre [(keyword? k)
          (var? avar)]}
   (MultiFn/.addMethod system k avar))

 (def effect-systems {component/info 'info
                      component/applicable? 'applicable?
                      component/handle 'handle})

 (defn install-effect [ns-sym]
   (let [ns (find-ns ns-sym)
         k (ns->k ns)]
     (doseq [[system fn-sym] effect-systems]
       (add-method system k (ns-resolve ns-sym fn-sym))))))

; => required & optional systems
; => required & optional attributes (select-keys on ns ?)

; * defc with just color & value-for-info can remove
; * key -> pretty-name ?
; * all component data in db ?
; what about tests ?

; * db & schema both components
; e.g. db is also keyword-attr
; => why not all component-attrs move to edn and editable ?!
; and can click and open source file ?!
; what does that mean for :property/id ? why does something need to keep its own id ?

; src/moon/property.clj
; property/def remove.
; its not tx/sound but audiovisual ?!
; if something has the same schema I can reuse the component ??
; stats take a look
