(ns ^:no-doc moon.install
  (:require [clojure.string :as str]
            [gdl.info :as info]
            [gdl.system :as system]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.entity.fsm :as fsm]
            [moon.player :as player]
            (moon.level generate
                        uf-caves
                        tiled-map)))

(defn- namespace->component-key [ns-str]
   (let [ns-parts (-> ns-str
                      (str/replace #"^moon." "")
                      (str/split #"\."))]
     (keyword (str/join "." (drop-last ns-parts))
              (last ns-parts))))

(comment
 (and (= (namespace->component-key "moon.effects.projectile")
         :effects/projectile)
      (= (namespace->component-key "moon.effects.target.convert")
         :effects.target/convert)))

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

(def ^:private effect
  {:required [#'effect/applicable?
              #'effect/handle]
   :optional [#'info/info
              #'effect/useful?
              #'effect/render]})

(install-all effect
             '[moon.effects.projectile
               moon.effects.spawn
               moon.effects.target-all
               moon.effects.target-entity

               moon.effects.target.audiovisual
               moon.effects.target.convert
               moon.effects.target.damage
               moon.effects.target.kill
               moon.effects.target.melee-damage
               moon.effects.target.spiderweb
               moon.effects.target.stun])

(def ^:private entity
  {:optional [#'info/info
              #'entity/->v
              #'entity/create
              #'entity/destroy
              #'entity/tick
              #'entity/render-below
              #'entity/render
              #'entity/render-above
              #'entity/render-info]})

(install-all entity
             '[moon.entity.alert-friendlies-after-duration
               moon.entity.animation
               moon.entity.clickable
               moon.entity.delete-after-animation-stopped
               moon.entity.delete-after-duration
               moon.entity.destroy-audiovisual
               moon.entity.faction
               moon.entity.fsm
               moon.entity.image
               moon.entity.inventory
               moon.entity.line-render
               moon.entity.mouseover?
               moon.entity.projectile-collision
               moon.entity.skills
               moon.entity.string-effect
               moon.entity.modifiers
               moon.entity.movement
               moon.entity.temp-modifier
               moon.entity.hp
               moon.entity.mana])

(def ^:private entity-state
  (merge-with concat
              entity
              {:optional [#'fsm/enter
                          #'fsm/exit
                          #'fsm/cursor
                          #'player/pause-game?
                          #'player/manual-tick
                          #'player/clicked-inventory-cell
                          #'player/clicked-skillmenu-skill
                          #'player/draw-gui-view]}))

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
