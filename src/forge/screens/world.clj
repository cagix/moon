(ns forge.screens.world
  (:require [forge.graphics :as g :refer [draw-tiled-map draw-on-world-view gui-mouse-position world-camera world-mouse-position]]
            [forge.level :as level]
            [forge.ui.error-window :refer [error-window!]]
            [forge.graphics.camera :as cam]
            [forge.screen :as screen]
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.controls :as controls]
            [forge.entity.components :as entity]
            [forge.entity.state :as state]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.dev-menu :as dev-menu]
            [forge.ui.entity-info-window :as entity-info-window]
            [forge.ui.hp-mana :as hp-mana]
            [forge.ui.inventory :as inventory]
            [forge.ui.player-message :as player-message]
            [forge.world :as world :refer [explored-tile-corners tick-error paused? player-eid mouseover-entity]]
            [forge.world.debug-render :as debug-render]
            [forge.world.potential-fields :refer [update-potential-fields!]]
            [forge.world.raycaster :refer [ray-blocked?]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)))

(def ^:private explored-tile-color (gdx-color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter* [light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

(declare start)

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (.getUserObject actor)))

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
             :items (for [world (build-all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(start world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover-entity)] (:entity/id entity))
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
                     :update-fn frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (.getTable (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
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
  (:windows (screen-stage)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (ui/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (ui/children (windows))]
    (when (some visible? windows)
      (run! #(Actor/.setVisible % false) windows))))

(defn start [world-props]
  (change-screen :screens/world)
  (reset-stage (widgets))
  (world/clear)
  (world/init (level/generate world-props)))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (world/point->entities
                      (world-mouse-position)))]
    (->> world/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(world/line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when world/mouseover-eid
      (swap! world/mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'world/mouseover-eid new-eid)))

(defn- update-world []
  (state/manual-tick (entity/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root #'paused? (or tick-error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @player-eid))
                                (not (controls/unpaused?)))))
  (when-not paused?
    (let [delta-ms (min (delta-time) world/max-delta-time)]
      (alter-var-root #'world/elapsed-time + delta-ms)
      (bind-root #'world/delta delta-ms) )
    (let [entities (world/active-entities)]
      (update-potential-fields! entities)
      (try (world/tick-entities entities)
           (catch Throwable t
             (error-window! t)
             (bind-root #'tick-error t)))))
  (world/remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world/tiled-map
                  (tile-color-setter (cam/position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (world/render-entities (map deref (world/active-entities)))
                       (debug-render/after-entities))))

(deftype WorldScreen []
  screen/Screen
  (enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (exit [_]
    (g/set-cursor :cursors/default))

  (render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (destroy [_]
    (world/clear)))

(defn screen []
  {:screen (->WorldScreen)})
