(ns moon.start
  (:require [moon.db :as db]
            [gdl.graphics.color :as color]
            [moon.info :as info]
            [gdl.stage :as stage]
            [moon.system :as system]
            [gdl.utils :as utils :refer [k->pretty-name readable-number]]
            [moon.app :as core :refer [batch gui-viewport]]
            [moon.entity :as entity]
            [moon.systems.component :as component]
            [moon.systems.effect :as effect]
            [moon.systems.entity :as entity-sys]
            [moon.systems.entity-state :as state]
            [moon.screens.editor :as editor]
            [moon.screens.main :as main-menu]
            [moon.screens.map-editor :as map-editor]
            [moon.screens.minimap :as minimap]
            [moon.screens.world :as world]
            (moon.schema animation
                         boolean
                         enum
                         image
                         map
                         number
                         one-to-many
                         one-to-one
                         sound
                         string)
            (moon.level generate
                        uf-caves
                        tiled-map)))

(def ^:private effect
  {:required [#'effect/applicable?
              #'effect/handle]
   :optional [#'component/info
              #'effect/useful?
              #'effect/render]})

(defn- install-effects [ns-syms]
  (doseq [ns-sym ns-syms]
    (system/install effect
                    ns-sym
                    (system/namespace->component-key #"^methods." (str ns-sym)))))

(install-effects
 '[methods.effects.projectile
   methods.effects.spawn
   methods.effects.target-all
   methods.effects.target-entity

   methods.effects.target.audiovisual
   methods.effects.target.convert
   methods.effects.target.damage
   methods.effects.target.kill
   methods.effects.target.melee-damage
   methods.effects.target.spiderweb
   methods.effects.target.stun])


; TODO check _only_ systems as public fns (private?)

(def ^:private entity
  {:optional [#'component/info
              #'entity-sys/->v
              #'entity-sys/create
              #'entity-sys/destroy
              #'entity-sys/tick
              #'entity-sys/render-below
              #'entity-sys/render
              #'entity-sys/render-above
              #'entity-sys/render-info]})

(system/install-all entity
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
                      moon.entity.species
                      moon.entity.string-effect
                      moon.entity.modifiers
                      moon.entity.movement
                      moon.entity.temp-modifier
                      moon.entity.hp
                      moon.entity.mana])

(def ^:private entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

(system/install entity-state 'moon.entity.npc.dead              :npc-dead)
(system/install entity-state 'moon.entity.npc.idle              :npc-idle)
(system/install entity-state 'moon.entity.npc.moving            :npc-moving)
(system/install entity-state 'moon.entity.npc.sleeping          :npc-sleeping)
(system/install entity-state 'moon.entity.player.dead           :player-dead)
(system/install entity-state 'moon.entity.player.idle           :player-idle)
(system/install entity-state 'moon.entity.player.item-on-cursor :player-item-on-cursor)
(system/install entity-state 'moon.entity.player.moving         :player-moving)
(system/install entity-state 'moon.entity.active                :active-skill)
(system/install entity-state 'moon.entity.stunned               :stunned)

(color/put "MODIFIERS" :cyan)
(color/put "PRETTY_NAME" [0.84 0.8 0.52])

(defmethod component/info :property/pretty-name [[_ value]]
  (str "[PRETTY_NAME]"value"[]"))

(defmethod component/info :maxrange [[_ maxrange]]
  (str "[LIGHT_GRAY]Range " maxrange " meters[]"))

(defmethod component/info :creature/level [[_ lvl]]
  (str "[GRAY]Level " lvl "[]"))

(defmethod component/info :projectile/piercing? [_] ; TODO also when false ?!
  "[LIME]Piercing[]")

(defmethod component/info :skill/action-time-modifier-key [[_ v]]
  (str "[VIOLET]" (case v
                    :entity/cast-speed "Spell"
                    :entity/attack-speed "Attack") "[]"))

(defmethod component/info :skill/action-time [[_ v]]
  (str "[GOLD]Action-Time: " (readable-number v) " seconds[]"))

(defmethod component/info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "[SKY]Cooldown: " (readable-number v) " seconds[]")))

(defmethod component/info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "[CYAN]Cost: " v " Mana[]")))

(defmethod component/info ::stat [[k _]]
  (str (k->pretty-name k) ": " (entity/stat info/*entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(def ^:private config
  {:app-config {:title "Moon"
                :fps 60
                :width 1440
                :height 900
                :dock-icon "moon.png"}
   :asset-folder "resources/"
   :cursors {:cursors/bag                   ["bag001"       [0   0]]
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
             :cursors/walking               ["walking"      [16 16]]}
   :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                  :size 16
                  :quality-scaling 2}
   :tile-size 48
   :world-viewport-width 1440
   :world-viewport-height 900
   :gui-viewport-width 1440
   :gui-viewport-height 900
   :ui-skin-scale :skin-scale/x1
   :init-screens (fn []
                   {:screens/main-menu  (stage/create gui-viewport batch (main-menu/create))
                    :screens/map-editor (stage/create gui-viewport batch (map-editor/create))
                    :screens/editor     (stage/create gui-viewport batch (editor/create))
                    :screens/minimap    (minimap/create)
                    :screens/world      (stage/create gui-viewport batch (world/create))})
   :first-screen-k :screens/main-menu})

(defn -main []
  (db/init :schema "schema.edn" :properties "properties.edn")
  (core/start-app config))
