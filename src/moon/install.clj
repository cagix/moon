(ns ^:no-doc moon.install
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.system :as system]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.components :as components]
            [moon.entity :as entity]
            [moon.item :as item]
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
            moon.stats
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

(defn- install
  ([component-systems ns-sym]
   (system/install component-systems
                   ns-sym
                   (namespace->component-key (str ns-sym))))
  ([component-systems ns-sym k]
   (system/install component-systems ns-sym k)))

(defn- install-all [component-systems ns-syms]
  (doseq [ns-sym ns-syms]
    (install component-systems ns-sym)) )

(install-all components/effect
             '[moon.effect.projectile
               moon.effect.spawn
               moon.effect.target-all
               moon.effect.target-entity
               moon.effect.entity.convert
               moon.effect.entity.damage
               moon.effect.entity.kill
               moon.effect.entity.melee-damage
               moon.effect.entity.spiderweb
               moon.effect.entity.stun])

(install-all components/fsm
             '[moon.fsms.player
               moon.fsms.npc])

(install components/entity 'moon.entity.alert-friendlies-after-duration)
(install components/entity 'moon.entity.animation)
(install components/entity 'moon.entity.clickable)
(install components/entity 'moon.entity.delete-after-animation-stopped)
(install components/entity 'moon.entity.delete-after-duration)
(install components/entity 'moon.entity.destroy-audiovisual)
(install components/entity 'moon.entity.faction)
(install components/entity 'moon.entity.fsm)
(install components/entity 'moon.entity.image)
(install components/entity 'moon.entity.inventory)
(install components/entity 'moon.entity.line-render)
(install components/entity 'moon.entity.mouseover :entity/mouseover?)
(install components/entity 'moon.entity.player :entity/player?)
(install components/entity 'moon.entity.projectile-collision)
(install components/entity 'moon.entity.skills)
(install components/entity 'moon.entity.string-effect)
(install components/entity 'moon.entity.modifiers)
(install components/entity 'moon.entity.movement)
(install components/entity 'moon.entity.temp-modifier)
(install components/entity 'moon.entity.stat :entity/stat)
(install components/entity 'moon.entity.hitpoints :stats/hp)
(install components/entity 'moon.entity.mana      :stats/mana)

(install components/entity-state 'moon.entity.npc.dead              :npc-dead)
(install components/entity-state 'moon.entity.npc.idle              :npc-idle)
(install components/entity-state 'moon.entity.npc.moving            :npc-moving)
(install components/entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(install components/entity-state 'moon.entity.player.dead           :player-dead)
(install components/entity-state 'moon.entity.player.idle           :player-idle)
(install components/entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(install components/entity-state 'moon.entity.player.moving         :player-moving)
(install components/entity-state 'moon.entity.active                :active-skill)
(install components/entity-state 'moon.entity.stunned               :stunned)

(install-all components/tx
             '[moon.tx.audiovisual
               moon.tx.creature
               moon.tx.cursor
               moon.tx.effect
               moon.tx.item
               moon.tx.line-render
               moon.tx.player-modal
               moon.tx.projectile
               moon.tx.shout
               moon.tx.sound])

(install-all components/widget
             '[moon.widgets.action-bar
               moon.widgets.dev-menu
               moon.widgets.entity-info-window
               moon.widgets.hp-mana
               moon.widgets.inventory
               moon.widgets.player-message
               moon.widgets.properties-overview
               moon.widgets.property])

(defmethod component/info :maxrange [[_ maxrange]]
  (str "[LIGHT_GRAY]Range " maxrange " meters[]"))

(defmethod component/info :property/pretty-name [[_ value]]
  (str "[PRETTY_NAME]"value"[]"))

(defmethod entity/->v :creature/species [[_ species]]
  (str/capitalize (name species)))

(defmethod component/info :creature/species [[_ species]]
  (str "[LIGHT_GRAY]Creature - " species "[]"))

(defmethod component/info :creature/level [[_ lvl]]
  (str "[GRAY]Level " lvl "[]"))

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
