(ns moon.components
  (:require [clojure.string :as str]
            [moon.component :as component]
            [moon.entity :as entity]
            (moon.entity inventory
                         modifiers)
            (moon.level generate
                        uf-caves
                        tiled-map)
            (moon.operation inc
                            mult
                            val-max)
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
            (moon.tx entity)
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

  ; TODO check fn-params ... ? compare with sys ?
  #_(first (:arglists (meta #'render)))
  (alter-meta! avar assoc :no-doc true)
  (MultiFn/.addMethod system k avar))

(defn- ns-publics-without-no-doc? [ns]
  (some #(not (:no-doc (meta %))) (vals (ns-publics ns))))

(defn- add-methods
  ([component-systems ns-sym]
   (add-methods component-systems
                ns-sym
                (namespace->component-key (str ns-sym))))

  ([component-systems ns-sym k]
   (require ns-sym)
   (let [resolve-method #(ns-resolve ns-sym (:name (meta %)))]
     (doseq [system-var (:required component-systems)
             :let [method-var (resolve-method system-var)]]
       (assert method-var)
       (add-method @system-var k method-var))
     (doseq [system-var (:optional component-systems)
             :let [method-var (resolve-method system-var)]
             :when method-var]
       (add-method @system-var k method-var)))
   (let [ns (find-ns ns-sym)]
     (when-not (ns-publics-without-no-doc? ns)
       (alter-meta! ns assoc :no-doc true)))))

(def ^:private effect
  {:required [#'component/applicable?
              #'component/handle]
   :optional [#'component/info
              #'component/useful?
              #'component/render]})

(doseq [ns-sym '[moon.effect.projectile
                 moon.effect.spawn
                 moon.effect.target-all
                 moon.effect.target-entity
                 moon.effect.entity.convert
                 moon.effect.entity.damage
                 moon.effect.entity.kill
                 moon.effect.entity.melee-damage
                 moon.effect.entity.spiderweb
                 moon.effect.entity.stun]]
  (add-methods effect ns-sym))

(def ^:private fsm
  {:required [#'component/create]})

(add-methods fsm 'moon.fsms.player)
(add-methods fsm 'moon.fsms.npc)

(def ^:private entity
  {:optional [#'component/info
              #'component/handle
              #'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render
              #'entity/render-above
              #'entity/render-info]})

(add-methods entity 'moon.entity.animation)
(add-methods entity 'moon.entity.clickable)
(add-methods entity 'moon.entity.delete-after-animation-stopped)
(add-methods entity 'moon.entity.delete-after-duration)
(add-methods entity 'moon.entity.destroy-audiovisual)
(add-methods entity 'moon.entity.faction)
(add-methods entity 'moon.entity.fsm)
(add-methods entity 'moon.entity.image)
(add-methods entity 'moon.entity.line-render)
(add-methods entity 'moon.entity.mouseover :entity/mouseover?)
(add-methods entity 'moon.entity.player :entity/player?)
(add-methods entity 'moon.entity.projectile-collision)
(add-methods entity 'moon.entity.skills)
(add-methods entity 'moon.entity.string-effect)
(add-methods entity 'moon.entity.movement)
(add-methods entity 'moon.entity.temp-modifier)
(add-methods entity 'moon.entity.hitpoints :stats/hp)

(def ^:private entity-state
  (merge-with concat
              entity
              {:optional [#'entity/enter
                          #'entity/exit
                          #'entity/player-enter
                          #'entity/pause-game?
                          #'entity/manual-tick
                          #'entity/clicked-inventory-cell
                          #'entity/clicked-skillmenu-skill
                          #'entity/draw-gui-view]}))

(add-methods entity-state 'moon.entity.npc.dead              :npc-dead)
(add-methods entity-state 'moon.entity.npc.idle              :npc-idle)
(add-methods entity-state 'moon.entity.npc.moving            :npc-moving)
(add-methods entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(add-methods entity-state 'moon.entity.player.dead           :player-dead)
(add-methods entity-state 'moon.entity.player.idle           :player-idle)
(add-methods entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(add-methods entity-state 'moon.entity.player.moving         :player-moving)
(add-methods entity-state 'moon.entity.active                :active-skill)
(add-methods entity-state 'moon.entity.stunned               :stunned)

(def ^:private tx
  {:required [#'component/handle]})

(add-methods tx 'moon.tx.audiovisual)
(add-methods tx 'moon.tx.creature)
(add-methods tx 'moon.tx.cursor)
(add-methods tx 'moon.tx.effect)
(add-methods tx 'moon.tx.item)
(add-methods tx 'moon.tx.line-render)
(add-methods tx 'moon.tx.projectile)
(add-methods tx 'moon.tx.sound)
