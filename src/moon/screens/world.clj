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
            [gdl.utils :refer [readable-number dev-mode?]]
            [moon.app :refer [draw-tiled-map draw-on-world-view gui-mouse-position set-cursor stage world-camera world-mouse-position change-screen]]
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
            [moon.world :as world :refer [tick-error paused? player-eid]]
            [moon.world.debug-render :as debug-render]
            [moon.world.mouseover :as mouseover]
            [moon.world.potential-fields :refer [update-potential-fields!]]
            [moon.world.tile-color-setter :as tile-color-setter]))

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
  (world/clear)
  (world/init (level/generate world-id)))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- update-world []
  (player/update-state)
  (mouseover/update) ; this do always so can get debug info even when game not running
  (.bindRoot #'paused? (or tick-error
                           (and pausing?
                                (player/state-pauses-game?)
                                (not (controls/unpaused?)))))
  (when-not paused?
    (let [delta-ms (min (delta-time) movement/max-delta-time)]
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

(defn create []
  {:screen (->WorldScreen)})
