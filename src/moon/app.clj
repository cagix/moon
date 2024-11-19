(ns moon.app
  (:require [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.app :as app]
            [gdl.db :as db]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.color :as color]
            [gdl.graphics.cursors :as cursors]
            [gdl.graphics.tiled :as graphics.tiled]
            [gdl.graphics.text :as font]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.world-view :as world-view]
            [gdl.info :as info :refer [info]]
            gdl.schemas
            [gdl.screen :as screen]
            [gdl.screens.editor :as editor]
            [gdl.stage :as stage]
            [gdl.system :as system]
            [gdl.ui :as ui]
            [gdl.utils :refer [k->pretty-name readable-number]]
            [moon.core :refer [batch]]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.entity.fsm :as fsm]
            [moon.entity.stat :as stat]
            [moon.screens.main :as main-menu]
            [moon.screens.map-editor :as map-editor]
            [moon.screens.minimap :as minimap]
            [moon.screens.world :as world]
            [moon.player :as player]
            (moon.level generate
                        uf-caves
                        tiled-map))
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

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

(color/put "MODIFIERS" :cyan)
(color/put "PRETTY_NAME" [0.84 0.8 0.52])

(defmethod info :property/pretty-name [[_ value]]
  (str "[PRETTY_NAME]"value"[]"))

(defmethod info :maxrange [[_ maxrange]]
  (str "[LIGHT_GRAY]Range " maxrange " meters[]"))

(defmethod info :entity/species [[_ species]]
  (str "[LIGHT_GRAY]Creature - " (str/capitalize (name species)) "[]"))

(defmethod info :creature/level [[_ lvl]]
  (str "[GRAY]Level " lvl "[]"))

(defmethod info :projectile/piercing? [_] ; TODO also when false ?!
  "[LIME]Piercing[]")

(defmethod info :skill/action-time-modifier-key [[_ v]]
  (str "[VIOLET]" (case v
                    :entity/cast-speed "Spell"
                    :entity/attack-speed "Attack") "[]"))

(defmethod info :skill/action-time [[_ v]]
  (str "[GOLD]Action-Time: " (readable-number v) " seconds[]"))

(defmethod info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "[SKY]Cooldown: " (readable-number v) " seconds[]")))

(defmethod info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "[CYAN]Cost: " v " Mana[]")))

(defmethod info ::stat [[k _]]
  (str (k->pretty-name k) ": " (stat/value info/*entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(bind-root #'gdl.schema.map/property-k-sort-order
           [:property/id
            :property/pretty-name
            :entity/image
            :entity/animation
            :entity/species
            :creature/level
            :entity/body
            :item/slot
            :projectile/speed
            :projectile/max-range
            :projectile/piercing?
            :skill/action-time-modifier-key
            :skill/action-time
            :skill/start-action-sound
            :skill/cost
            :skill/cooldown])

(bind-root #'gdl.editor.overview/overview
           {:properties/audiovisuals {:columns 10
                                      :image/scale 2}
            :properties/creatures {:columns 15
                                   :image/scale 1.5
                                   :sort-by-fn #(vector (:creature/level %)
                                                        (name (:entity/species %))
                                                        (name (:property/id %)))
                                   :extra-info-text #(str (:creature/level %))}
            :properties/items {:columns 20
                               :image/scale 1.1
                               :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                      (name slot)
                                                      "")
                                                    (name (:property/id %)))}
            :properties/projectiles {:columns 16
                                     :image/scale 2}
            :properties/skills {:columns 16
                                :image/scale 2}
            :properties/worlds {:columns 10}})

(bind-root #'gdl.info/info-text-k-order
           [:property/pretty-name
            :skill/action-time-modifier-key
            :skill/action-time
            :skill/cooldown
            :skill/cost
            :skill/effects
            :entity/species
            :creature/level
            :entity/hp
            :entity/mana
            :entity/strength
            :entity/cast-speed
            :entity/attack-speed
            :entity/armor-save
            :entity/delete-after-duration
            :projectile/piercing?
            :entity/projectile-collision
            :maxrange
            :entity-effects])

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (app/start {:title "Moon"
              :fps 60
              :width 1440
              :height 900
              :dock-icon "moon.png"}
             (reify app/Listener
               (create [_]
                 (assets/init "resources/")
                 (.bindRoot #'batch (SpriteBatch.))
                 (shape-drawer/init batch)
                 (cursors/init {:cursors/bag                   ["bag001"       [0   0]]
                                :cursors/black-x               ["black_x"      [0   0]]
                                :cursors/default               ["default"      [0   0]]
                                :cursors/denied                ["denied"       [16 16]]
                                :cursors/hand-before-grab      ["hand004"      [4  16]]
                                :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                :cursors/hand-grab             ["hand003"      [4  16]]
                                :cursors/move-window           ["move002"      [16 16]]
                                :cursors/no-skill-selected     ["denied003"    [0   0]]
                                :cursors/over-button           ["hand002"      [0   0]]
                                :cursors/sandclock             ["sandclock"    [16 16]]
                                :cursors/skill-not-usable      ["x007"         [0   0]]
                                :cursors/use-skill             ["pointer004"   [0   0]]
                                :cursors/walking               ["walking"      [16 16]]})
                 (gui-view/init {:world-width 1440
                                 :world-height 900})
                 (world-view/init {:world-width 1440
                                   :world-height 900
                                   :tile-size 48})
                 (ui/load! :skin-scale/x1)
                 (graphics.tiled/init batch)
                 (font/init {:file "fonts/exocet/films.EXL_____.ttf"
                             :size 16
                             :quality-scaling 2})
                 (screen/set-screens
                  {:screens/main-menu  (stage/create batch (main-menu/create))
                   :screens/map-editor (stage/create batch (map-editor/create))
                   :screens/editor     (stage/create batch (editor/create))
                   :screens/minimap    (minimap/create)
                   :screens/world      (stage/create batch (world/create))})
                 (world/start :worlds/vampire))

               (dispose [_]
                 (assets/dispose)
                 (.dispose batch)
                 (shape-drawer/dispose)
                 (cursors/dispose)
                 (ui/dispose!)
                 (font/dispose)
                 (screen/dispose-all))

               (render [_]
                 (clear-screen :black)
                 (screen/render (screen/current)))

               (resize [_ dimensions]
                 (gui-view/resize   dimensions)
                 (world-view/resize dimensions)))))
