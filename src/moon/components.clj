(ns moon.components
  ; can I just convert them all ???
  ; * only one defc
  ; * no other public functions or anything else

  ; What to do with :schema ? & properties ?
  ; => could be done inside the editor  / schema
  ; => could define properties/attributes & add/remove
  ; => have a game database editor

  ; => tests are important that way my application stays decomplected etc. design stays good
  ; => e.g. can I use property editor separately
  ; that makes me think - add tests to gdl
  ; release w. tests ?!
  ; that scool
  ; how would apotential field test look like ?

  ; that makes me think of the one where the army spawns automatically
  ; and you can do stuff idk
  ; or youre in the middle of a huge fight

  (:require (moon.effect projectile
                         spawn
                         target-all
                         target-entity) ; :entity-effects, :maxrange
            (moon.effect.entity convert
                                damage ; defc :damage/min-max schema: :s/val-max
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
                                item-on-cursor ; :widgets/draw-item-on-cursor
                                moving)
            (moon.entity active
                         animation
                         clickable
                         delete-after-animation-stopped
                         delete-after-duration
                         destroy-audiovisual
                         faction ; defn enemy
                         follow-ai ; not a 'component'
                         fsm
                         hitpoints ; :stats/hp not :entity/hitpoints
                         image
                         inventory ; lots of :tx
                         line-render
                         modifiers ; lots of stuff
                         mouseover
                         movement ; tx/set-movement, max-delta-time, speed-schema
                         player ; eid
                         projectile
                         skills ; tx's, has-skill ?
                         string-effect ; tx's
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

            ; they have lots of mixed up stuff - schema is plain data ? ....
            (moon.properties audiovisuals
                             creatures
                             items
                             projectiles
                             skills
                             worlds)

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

            (moon.screens editor
                          main
                          map-editor
                          minimap
                          world) ; more stuff

            (moon.tx audiovisual
                     creature
                     cursor
                     effect
                     entity ; lots o stuff
                     item
                     line-render
                     projectile
                     sound
                     spawn-creatures)

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
         k (ns->k ns)
         attr-map (meta *ns*)]
     (assert (:schema attr-map))
     (defc* k (select-keys attr-map [:schema]))
     (doseq [[system fn-sym] effect-systems]
       (add-method system k (ns-resolve ns-sym fn-sym))))))

; => required & optional systems
; => required & optional attributes (select-keys on ns ?)

(comment

 (defn- for-print [schema]
   (cond
     (= schema number?)  'number?
     (= schema nat-int?) 'nat-int?
     (= schema int?)     'int?
     (= schema pos?)     'pos?
     (= schema pos-int?) 'pos-int?
     :else schema))

 (binding [*print-level* nil]
   (->> (into {}
              (for [[k v] (filter (comp :schema val) component-attrs)]
                [k (for-print (:schema v))]))
        (into (sorted-map))
        clojure.pprint/pprint
        with-out-str
        (spit "resources/schema.edn")))


 )

(comment
 (distinct (mapcat keys (vals component-attrs)))

 (:params
  :schema
  :overview
  :modifier-ops
  :effect-ops
  :editor/doc
  :doc)

 )
