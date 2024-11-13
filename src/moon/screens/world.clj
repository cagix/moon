(ns moon.screens.world
  (:require [data.grid2d :as g2d]
            [gdl.db :as db]
            [gdl.graphics :refer [frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.graphics.cursors :as cursors]
            [gdl.graphics.image :as img]
            [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.world-view :as world-view]
            [gdl.screen :as screen]
            [gdl.stage :as stage]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.utils :refer [readable-number tile->middle dev-mode?]]
            [gdl.widgets.error-window :refer [error-window!]]
            [moon.controls :as controls]
            [moon.entity.movement :as movement]
            [moon.level :as level]
            [moon.player :as player]
            [moon.widgets.action-bar :as action-bar]
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
            [moon.world.time :as time])
  (:import (com.kotcrab.vis.ui.widget Menu MenuItem MenuBar)))

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

(defn- spawn-creatures [{:keys [tiled-map start-position]}]
  (doseq [creature (cons {:position start-position
                          :creature-id :creatures/vampire
                          :components {:entity/fsm {:fsm :fsms/player
                                                    :initial-state :player-idle}
                                       :entity/faction :good
                                       :entity/player? true
                                       :entity/free-skill-points 3
                                       :entity/clickable {:type :clickable/player}
                                       :entity/click-distance-tiles 1.5}}
                         (when spawn-enemies?
                           (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                             {:position position
                              :creature-id (keyword creature-id)
                              :components {:entity/fsm {:fsm :fsms/npc
                                                        :initial-state :npc-sleeping}
                                           :entity/faction :evil}})))]
    (entities/creature (update creature :position tile->middle))))

(defn- menu-item [text on-clicked]
  (doto (MenuItem. text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (img/image (str "images/" icon ".png")) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table label))))))

(defn- add-debug-infos [mb]
  (let [table (.getTable mb)
        add! #(add-upd-label table %)]
    ;"Mouseover-Actor: "
    #_(when-let [actor (mouse-on-actor?)]
        (str "TRUE - name:" (.getName actor)
             "id: " (actor/id actor)))
    (add-upd-label table
                   #(str "Mouseover-entity id: " (when-let [entity (mouseover/entity)] (:entity/id entity)))
                   "mouseover")
    (add-upd-label table
                   #(str "elapsed-time " (readable-number time/elapsed) " seconds")
                   "clock")
    (add! #(str "paused? " paused?))
    (add! #(str "GUI: " (gui-view/mouse-position)))
    (add! #(str "World: "(mapv int (world-view/mouse-position))))
    (add-upd-label table
                   #(str "Zoom: " (cam/zoom (world-view/camera)))
                   "zoom")
    (add-upd-label table
                   #(str "FPS: " (gdl.graphics/frames-per-second))
                   "fps")))

(declare start)

(defn- ->menu-bar []
  (let [menu-bar (MenuBar.)
        app-menu (Menu. "App")]
    (.addItem app-menu (menu-item "Map editor" (partial screen/change :screens/map-editor)))
    (.addItem app-menu (menu-item "Properties" (partial screen/change :screens/editor)))
    (.addItem app-menu (menu-item "Exit"       (partial screen/change :screens/main-menu)))
    (.addMenu menu-bar app-menu)
    (let [world (Menu. "World")]
      (doseq [{:keys [property/id]} (db/all :properties/worlds)]
        (.addItem world (menu-item (str "Start " id) #(start id))))
      (.addMenu menu-bar world))
    (let [help (Menu. "Help")]
        (.addItem help (MenuItem. controls/help-text))
      (.addMenu menu-bar help))
    (add-debug-infos menu-bar)
    menu-bar))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (.getTable (->menu-bar))
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
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (ui/children (windows))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible! % false) windows))))

(defn start [world-id]
  (screen/change :screens/world)
  (stage/reset (widgets))
  (let [{:keys [tiled-map] :as level} (level/generate world-id)]
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
    (spawn-creatures level)))

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
  (cam/set-position! (world-view/camera) (:position @player/eid))
  ; FIXME position DRY
  (tiled-map/render (cam/position (world-view/camera)))
  (world-view/render (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (entities/render (map deref (entities/active)))
                       (debug-render/after-entities))))

(deftype WorldScreen []
  screen/Screen
  (screen/enter [_]
    (cam/set-zoom! (world-view/camera) 0.8))

  (screen/exit [_]
    (cursors/set :cursors/default))

  (screen/render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (screen/change :screens/minimap)))

  (screen/dispose [_]
    (tiled-map/clear)))

(defn create []
  (stage/create :screen (->WorldScreen)))
