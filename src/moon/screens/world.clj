(ns moon.screens.world
  (:require [data.grid2d :as g2d]
            [moon.db :as db]
            [gdl.graphics :refer [frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.screen :as screen]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.stage :as stage]
            [gdl.utils :refer [readable-number tile->middle dev-mode?]]
            [moon.core :refer [draw-on-world-view gui-mouse-position set-cursor stage world-camera world-mouse-position change-screen]]
            [moon.controls :as controls]
            [moon.entity.movement :as movement]
            [moon.level :as level]
            [moon.player :as player]
            [moon.widgets.error-window :refer [error-window!]]
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.dev-menu :as dev-menu]
            [moon.widgets.entity-info-window :as entity-info-window]
            [moon.widgets.hp-mana :as hp-mana]
            [moon.widgets.inventory :as inventory]
            [moon.widgets.player-message :as player-message]
            [moon.world.content-grid :as content-grid]
            [moon.world.debug-render :as debug-render]
            [moon.world.entities :as entities]
            [moon.world.grid :as grid]
            [moon.world.mouseover :as mouseover]
            [moon.world.potential-fields :refer [update-potential-fields!]]
            [moon.world.raycaster :as raycaster]
            [moon.world.tiled-map :as tiled-map]
            [moon.world.time :as time]))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/width tiled-map)
   (tiled/height tiled-map)
   (fn [position]
     (atom (grid/->cell position
                        (case (level/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(declare tick-error
         paused?)

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- spawn-player [start-position]
  (entities/creature {:position (tile->middle start-position)
                      :creature-id :creatures/vampire
                      :components {:entity/fsm {:fsm :fsms/player
                                                :initial-state :player-idle}
                                   :entity/faction :good
                                   :entity/player? true
                                   :entity/free-skill-points 3
                                   :entity/clickable {:type :clickable/player}
                                   :entity/click-distance-tiles 1.5}}))

(defn- spawn-enemies [tiled-map]
  (doseq [creature (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                     {:position position
                      :creature-id (keyword creature-id)
                      :components {:entity/fsm {:fsm :fsms/npc
                                                :initial-state :npc-sleeping}
                                   :entity/faction :evil}})]
    (entities/creature (update creature :position tile->middle))))

(declare start)

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
             :items (for [{:keys [property/id]} (db/all :properties/worlds)]
                      {:label (str "Start " id)
                       :on-click #(start id)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover/entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number time/elapsed) " seconds")
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
                     :update-fn gdl.graphics/frames-per-second
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
   (ui/actor {:draw player/draw-state})
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

(defn start [world-id]
  (change-screen :screens/world)
  (stage/reset (stage) (widgets))
  (let [{:keys [tiled-map start-position]} (level/generate world-id)]
    (tiled-map/clear)
    (tiled-map/init tiled-map)
    (bind-root #'grid/grid (create-grid tiled-map))
    (raycaster/init grid/grid grid/blocks-vision?)
    (let [width  (tiled/width  tiled-map)
          height (tiled/height tiled-map)]
      (bind-root #'entities/content-grid (content-grid/create {:cell-size 16  ; FIXME global config
                                                               :width  width
                                                               :height height})))
    (bind-root #'tick-error nil)
    (bind-root #'entities/ids->eids {})
    (time/init)
    (bind-root #'player/eid (spawn-player start-position))
    (when spawn-enemies?
      (spawn-enemies tiled-map))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- update-game-paused []
  (bind-root #'paused? (or tick-error
                           (and pausing?
                                (player/state-pauses-game?)
                                (not (controls/unpaused?)))))
  nil)

(defn- update-world []
  (player/update-state)
  (mouseover/update) ; this do always so can get debug info even when game not running
  (update-game-paused)
  (when-not paused?
    (time/pass (min (delta-time) movement/max-delta-time))
    (let [entities (entities/active)]
      (update-potential-fields! entities)
      (try (entities/tick entities)
           (catch Throwable t
             (error-window! t)
             (bind-root #'tick-error t)))))
  (entities/remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player/eid))
  ; FIXME position DRY
  (tiled-map/render (cam/position (world-camera)))
  (draw-on-world-view (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (entities/render (map deref (entities/active)))
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
    (tiled-map/clear)))

(defn create []
  {:screen (->WorldScreen)})
