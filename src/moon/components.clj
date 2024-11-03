(ns ^:no-doc moon.components
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.item :as item]
            [moon.modifiers :as mods]
            (moon.level generate
                        uf-caves
                        tiled-map)
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
            (moon.tx entity)))

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
  (assert (keyword? k))
  (assert (var? avar) (pr-str avar))
  (when (k (methods system))
    (println "WARNING: Overwriting method" (:name (meta avar)) "on" k))

  ; TODO check fn-params ... ? compare with sys ?
  #_(first (:arglists (meta #'render)))

  (alter-meta! avar assoc :no-doc true)
  (clojure.lang.MultiFn/.addMethod system k avar))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (add-method @system-var k method-var))))

(defn- ns-publics-without-no-doc? [ns]
  (some #(not (:no-doc (meta %))) (vals (ns-publics ns))))

(defn- install
  ([component-systems ns-sym]
   (install component-systems
            ns-sym
            (namespace->component-key (str ns-sym))))

  ([component-systems ns-sym k]
   (require ns-sym)
   (add-methods (:required component-systems) ns-sym k)
   (add-methods (:optional component-systems) ns-sym k :optional? true)
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
  (install effect ns-sym))

(def ^:private fsm
  {:required [#'component/create]})

(install fsm 'moon.fsms.player)
(install fsm 'moon.fsms.npc)

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

(install entity 'moon.entity.alert-friendlies-after-duration)
(install entity 'moon.entity.animation)
(install entity 'moon.entity.clickable)
(install entity 'moon.entity.delete-after-animation-stopped)
(install entity 'moon.entity.delete-after-duration)
(install entity 'moon.entity.destroy-audiovisual)
(install entity 'moon.entity.faction)
(install entity 'moon.entity.fsm)
(install entity 'moon.entity.image)
(install entity 'moon.entity.inventory)
(install entity 'moon.entity.line-render)
(install entity 'moon.entity.mouseover :entity/mouseover?)
(install entity 'moon.entity.player :entity/player?)
(install entity 'moon.entity.projectile-collision)
(install entity 'moon.entity.skills)
(install entity 'moon.entity.string-effect)
(install entity 'moon.entity.modifiers)
(install entity 'moon.entity.movement)
(install entity 'moon.entity.temp-modifier)
(install entity 'moon.entity.hitpoints :stats/hp)

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

(install entity-state 'moon.entity.npc.dead              :npc-dead)
(install entity-state 'moon.entity.npc.idle              :npc-idle)
(install entity-state 'moon.entity.npc.moving            :npc-moving)
(install entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(install entity-state 'moon.entity.player.dead           :player-dead)
(install entity-state 'moon.entity.player.idle           :player-idle)
(install entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(install entity-state 'moon.entity.player.moving         :player-moving)
(install entity-state 'moon.entity.active                :active-skill)
(install entity-state 'moon.entity.stunned               :stunned)

(def ^:private tx
  {:required [#'component/handle]})

(install tx 'moon.tx.audiovisual)
(install tx 'moon.tx.creature)
(install tx 'moon.tx.cursor)
(install tx 'moon.tx.effect)
(install tx 'moon.tx.item)
(install tx 'moon.tx.line-render)
(install tx 'moon.tx.player-modal)
(install tx 'moon.tx.projectile)
(install tx 'moon.tx.shout)
(install tx 'moon.tx.sound)

(def ^:private widget
  {:required [#'component/create]
   :optional [#'component/handle]})

(doseq [ns-sym '[moon.widgets.action-bar
                 moon.widgets.dev-menu
                 moon.widgets.entity-info-window
                 moon.widgets.hp-mana
                 moon.widgets.inventory
                 moon.widgets.player-message
                 moon.widgets.properties-overview
                 moon.widgets.property]]
  (install widget ns-sym))

(defmethod component/info :maxrange [[_ maxrange]]
  (str "[LIGHT_GRAY]Range " maxrange " meters[]"))

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from entity

(defmethod component/info :property/pretty-name [[_ value]]
  (str "[PRETTY_NAME]"value"[]"))

(defmethod entity/->v :creature/species [[_ species]]
  (str/capitalize (name species)))

(defmethod component/info :creature/species [[_ species]]
  (str "[LIGHT_GRAY]Creature - " species "[]"))

(defmethod component/info :creature/level [[_ lvl]]
  (str "[GRAY]Level " lvl "[]"))

(defmethod component/info :item/modifiers [[_ mods]]
  (mods/info mods))

; TODO speed is 10 tiles/s but I checked moves 8 tiles/sec ... after delta time change ?

; -> range needs to be smaller than potential field range (otherwise hitting someone who can't get back at you)
; -> first range check then ray ! otherwise somewhere in contentfield out of sight
#_(defc :projectile/max-range)
#_(defc :projectile/speed)

(defmethod component/info :projectile/piercing? [_]
  "[LIME]Piercing[]")

#_(defc :world/max-area-level) ; TODO <= map-size !?
#_(defc :world/spawn-rate) ; TODO <1 !

(defmethod component/info :skill/action-time-modifier-key [[_ v]]
  (str "[VIOLET]" (case v
                    :stats/cast-speed "Spell"
                    :stats/attack-speed "Attack") "[]"))

(defmethod component/info :skill/action-time [[_ v]]
  (str "[GOLD]Action-Time: " (readable-number v) " seconds[]"))

(defmethod component/info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "[SKY]Cooldown: " (readable-number v) " seconds[]")))

(defmethod component/info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "[CYAN]Cost: " v " Mana[]")))

(color/put "MODIFIERS" :cyan)
(color/put "PRETTY_NAME" [0.84 0.8 0.52])
