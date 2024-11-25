(ns app.start
  (:require [app.screens.editor :as editor]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [forge.app :refer [start-app draw-tiled-map draw-on-world-view gui-mouse-position set-cursor stage world-camera world-mouse-position change-screen]]
            [forge.db :as db]
            [forge.level :as level]
            (forge.schema animation boolean enum image map number one-to-many one-to-one sound string)
            [forge.system :as system]
            [forge.screen :as screen]
            [forge.widgets.error-window :refer [error-window!]]
            [forge.graphics :refer [frames-per-second delta-time]]
            [forge.graphics.camera :as cam]
            [forge.input :refer [key-just-pressed?]]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]
            [forge.ui.stage :as stage]
            [forge.utils :refer [readable-number dev-mode?]]
            (mapgen generate uf-caves tiled-map)
            [moon.controls :as controls]
            [moon.entity :as entity]
            [moon.systems.effect :as effect]
            [forge.entity :as entity-sys]
            [moon.systems.entity-state :as state]
            moon.methods.info
            [moon.widgets.background-image :as background-image]
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.dev-menu :as dev-menu]
            [moon.widgets.entity-info-window :as entity-info-window]
            [moon.widgets.hp-mana :as hp-mana]
            [moon.widgets.inventory :as inventory]
            [moon.widgets.player-message :as player-message]
            [moon.world :as world :refer [tick-error paused? player-eid]]
            [moon.world.debug-render :as debug-render]
            [moon.world.mouseover :as mouseover]
            [moon.world.potential-fields :refer [update-potential-fields!]]
            [moon.world.tile-color-setter :as tile-color-setter])
  (:import (com.badlogic.gdx Gdx)))

(declare start-world)

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (actor/id actor)))

(defn- dev-menu-bar []
  (dev-menu/create
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial change-screen :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial change-screen :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial change-screen :screens/main-menu)}]}
            {:label "World"
             :items (for [world (db/all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(start-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover/entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number world/elapsed-time) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] paused?)}
                    {:label "GUI"
                     :update-fn gui-mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (world-camera))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn forge.graphics/frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (.getTable (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (actor/set-touchable! :disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

; FIXME camera/viewport used @ line of sight & raycaster explored tiles
; fixed player viewing range use & for opponents too

(defn- widgets []
  [(if dev-mode?
     (dev-menu)
     (ui/actor {}))
   (ui/table {:rows [[{:actor (action-bar/create)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana/actor)
   (ui/group {:id :windows
              :actors [(entity-info-window/create)
                       (inventory/create)]})
   (ui/actor {:draw #(state/draw-gui-view (entity/state-obj @player-eid))})
   (player-message/create)])

(defn- windows []
  (:windows (stage)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (ui/children (windows))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible! % false) windows))))

(defn- start-world [world-props]
  (change-screen :screens/world)
  (stage/reset (stage) (widgets))
  (world/clear)
  (world/init (level/generate world-props)))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- update-world []
  (state/manual-tick (entity/state-obj @player-eid))
  (mouseover/update) ; this do always so can get debug info even when game not running
  (.bindRoot #'paused? (or tick-error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @player-eid))
                                (not (controls/unpaused?)))))
  (when-not paused?
    (let [delta-ms (min (delta-time) world/max-delta-time)]
      (alter-var-root #'world/elapsed-time + delta-ms)
      (.bindRoot #'world/delta delta-ms) )
    (let [entities (world/active-entities)]
      (update-potential-fields! entities)
      (try (world/tick-entities entities)
           (catch Throwable t
             (error-window! t)
             (.bindRoot #'tick-error t)))))
  (world/remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world/tiled-map
                  (tile-color-setter/create
                   world/explored-tile-corners
                   (cam/position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (world/render-entities (map deref (world/active-entities)))
                       (debug-render/after-entities))))

(deftype WorldScreen []
  screen/Screen
  (screen/enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (screen/exit [_]
    (set-cursor :cursors/default))

  (screen/render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (screen/dispose [_]
    (world/clear)))

(defn- world-screen []
  {:screen (->WorldScreen)})

(def ^:private effect
  {:required [#'effect/applicable?
              #'effect/handle]
   :optional [#'effect/useful?
              #'effect/render]})

(system/install-all effect
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
  {:optional [#'entity-sys/->v
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
                      moon.entity.fsm
                      moon.entity.image
                      moon.entity.inventory
                      moon.entity.line-render
                      moon.entity.mouseover?
                      moon.entity.projectile-collision
                      moon.entity.skills
                      moon.entity.string-effect
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

(defn- main-menu []
  {:actors [(background-image/create)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(start-world world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(change-screen :screens/editor))])
                        [(ui/text-button "Exit"
                                         #(.exit Gdx/app))]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (.exit Gdx/app)))})]
   :screen (reify screen/Screen
             (screen/enter [_]
               (set-cursor :cursors/default))
             (screen/exit [_])
             (screen/render [_])
             (screen/dispose [_]))})

(def ^:private config
  {:cursors {:cursors/bag                   ["bag001"       [0   0]]
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
   :tile-size 48
   :world-viewport-width 1440
   :world-viewport-height 900
   :gui-viewport-width 1440
   :gui-viewport-height 900
   :ui-skin-scale :skin-scale/x1
   :init-screens (fn []
                   {:screens/main-menu  (main-menu)
                    :screens/map-editor (map-editor/create)
                    :screens/editor     (editor/create)
                    :screens/minimap    (minimap/create)
                    :screens/world      (world-screen)})
   :first-screen-k :screens/main-menu})

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (start-app config))
