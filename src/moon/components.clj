(ns moon.components
  (:require (moon.entity.npc dead
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
                       npc)
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

(require '[clojure.string :as str])
(import clojure.lang.MultiFn)

(defn- namespace->component-key [ns-str]
   (let [ns-parts (-> ns-str
                      (str/replace #"^moon." "")
                      (str/split #"\."))]
     (keyword (str/join "." (drop-last ns-parts))
              (last ns-parts))))

(comment
 (and (= (namespace->component-key "moon.effect.projectile")
         :effect/projectile)
      (= (namespace->component-key "moon.effect.entity.convert")
         :effect.entity/convert)))

; TODO check if rede-ffing the public fns changes something.
(defn- add-method [system k avar]
  {:pre [(keyword? k)
         (var? avar)]}
  (when (k (methods system))
    (println "WARNING: Overwriting method" (:name (meta avar)) "on" k))
  (MultiFn/.addMethod system k avar))

(require '[moon.component :as component])

(def effect-systems {:required [#'component/applicable?
                                #'component/handle]
                     :optional [#'component/info
                                #'component/useful?
                                #'component/render]})

(defn add-methods [ns-sym component-systems]
  (require ns-sym)
  (let [k (namespace->component-key (str ns-sym))
        resolve-method #(ns-resolve ns-sym (:name (meta %)))]
    (doseq [system-var (:required component-systems)
            :let [method-var (resolve-method system-var)]]
      (assert method-var)
      (add-method @system-var k method-var))
    (doseq [system-var (:optional component-systems)
            :let [method-var (resolve-method system-var)]
            :when method-var]
      (add-method @system-var k method-var))))

(add-methods 'moon.effect.projectile    effect-systems)
(add-methods 'moon.effect.spawn         effect-systems)
(add-methods 'moon.effect.target-all    effect-systems)
(add-methods 'moon.effect.target-entity effect-systems)

(add-methods 'moon.effect.entity.convert      effect-systems)
(add-methods 'moon.effect.entity.damage       effect-systems)
(add-methods 'moon.effect.entity.kill         effect-systems)
(add-methods 'moon.effect.entity.melee-damage effect-systems)
(add-methods 'moon.effect.entity.spiderweb    effect-systems)
(add-methods 'moon.effect.entity.stun         effect-systems)

(comment

 (seq (.listFiles (io/file "src/moon/effect/entity/")))

 (MultiFn/.addMethod component/info        :foo nil)
 (ns-resolve 'moon.effect.entity.convert 'info)

 ; TODO useful?
 ; make clearer name - its used by AI _After_ applicable? call
 ; and AI _only_ uses skills
 ; no wait, useful ? is just useful
 ; the AI logic is the part I need to explain
 ; same like faction , it just is what it is
 ; but potential field does the faction magic

 ; moon.effect.projectile
 ; only applicable? useful? & handle

 )

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
