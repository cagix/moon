(ns moon.components
  (:require [clojure.string :as str]
            [moon.component :as component]
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
            moon.world.widgets)
  (:import clojure.lang.MultiFn))

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

(defn- add-method [system k avar]
  {:pre [(keyword? k)
         (var? avar)]}
  (when (k (methods system))
    (println "WARNING: Overwriting method" (:name (meta avar)) "on" k))
  (MultiFn/.addMethod system k avar))

(defn- add-methods [ns-sym component-systems]
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

(def ^:private effect
  {:required [#'component/applicable?
              #'component/handle]
   :optional [#'component/info
              #'component/useful?
              #'component/render]})

(add-methods 'moon.effect.projectile          effect)
(add-methods 'moon.effect.spawn               effect)
(add-methods 'moon.effect.target-all          effect)
(add-methods 'moon.effect.target-entity       effect)
(add-methods 'moon.effect.entity.convert      effect)
(add-methods 'moon.effect.entity.damage       effect)
(add-methods 'moon.effect.entity.kill         effect)
(add-methods 'moon.effect.entity.melee-damage effect)
(add-methods 'moon.effect.entity.spiderweb    effect)
(add-methods 'moon.effect.entity.stun         effect)

(def ^:private fsm
  {:required [#'component/create]})

(add-methods 'moon.fsms.player fsm)
(add-methods 'moon.fsms.npc    fsm)
