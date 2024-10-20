(ns app.screens.world
  (:require [component.db :as db]
            [gdx.app :as app]
            [gdx.graphics :as g]
            [gdx.graphics.camera :as 🎥]
            [gdx.input :refer [key-pressed? key-just-pressed?]]
            [gdx.ui :as ui]
            [gdx.ui.actor :as a]
            [gdx.screen :as screen]
            [gdx.ui.stage :as stage]
            [gdx.ui.stage-screen :as stage-screen :refer [stage-get]]
            [level.generate :as level]
            [utils.core :refer [dev-mode?]]
            [moon.creature :as creature]
            moon.creature.player.item-on-cursor
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.entity-info-window :as entity-info-window]
            [moon.widgets.hp-mana :as hp-mana-bars]
            [moon.widgets.inventory :as inventory]
            [moon.widgets.player-message :as player-message]
            moon.widgets.player-modal
            [world.core :as world]

            moon.audiovisual
            moon.projectile
            world.entity.animation
            world.entity.delete-after-duration
            world.entity.image
            world.entity.line
            world.entity.movement
            world.entity.string-effect
            world.effect.damage
            world.effect.entity
            world.effect.target
            world.entity.stats))

(defn- check-window-hotkeys []
  (doseq [[hotkey window-id] {:keys/i :inventory-window
                              :keys/e :entity-info-window}
          :when (key-just-pressed? hotkey)]
    (a/toggle-visible! (get (:windows (stage-get)) window-id))))

(defn- close-windows?! []
  (let [windows (ui/children (:windows (stage-get)))]
    (if (some a/visible? windows)
      (do
       (run! #(a/set-visible! % false) windows)
       true))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (🎥/set-zoom! camera (max 0.1 (+ (🎥/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- check-zoom-keys []
  (let [camera (g/world-camera)]
    (when (key-pressed? :keys/minus)  (adjust-zoom camera    zoom-speed))
    (when (key-pressed? :keys/equals) (adjust-zoom camera (- zoom-speed)))))

; TODO move to actor/stage listeners ? then input processor used ....
(defn- check-key-input []
  (check-zoom-keys)
  (check-window-hotkeys)
  (cond (key-just-pressed? :keys/escape)
        (close-windows?!)

        ; TODO not implementing StageSubScreen so NPE no screen-render!
        #_(key-just-pressed? :keys/tab)
        #_(screen/change! :screens/minimap)))

(deftype WorldScreen []
  screen/Screen
  (screen/enter! [_])

  (screen/exit! [_]
    (g/set-cursor! :cursors/default))

  (screen/render! [_]
    (world/tick!)
    (check-key-input))

  (screen/dispose! [_]))

(defn create []
  [:screens/world (stage-screen/create :screen (->WorldScreen))])

(declare world-actors)

(defn- reset-stage! []
  (let [stage (stage-get)] ; these fns to stage itself
    (stage/clear! stage)
    (run! #(stage/add! stage %) (world-actors))))

(defn start-game-fn [world-id]
  (fn []
    (screen/change! :screens/world)
    (reset-stage!)
    (let [level (level/generate-level world-id)]
      (world/init! (:tiled-map level))
      (creature/spawn-all level))))

(import 'com.kotcrab.vis.ui.widget.MenuBar)
(import 'com.kotcrab.vis.ui.widget.Menu)
(import 'com.kotcrab.vis.ui.widget.MenuItem)

(defn- menu-item [text on-clicked]
  (doto (MenuItem. text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label [table text-fn]
  (let [label (ui/label "")]
    (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
    (.expandX (.right (.add table label)))))

(defn- fps [] (str "FPS: " (g/frames-per-second)))


(defn- add-debug-infos [mb]
  (let [table (.getTable mb)
        add! #(add-upd-label table %)]
    ;"Mouseover-Actor: "
    #_(when-let [actor (mouse-on-actor?)]
        (str "TRUE - name:" (.getName actor)
             "id: " (a/id actor)))
    (add! #(str "Mouseover-entity id: " (when-let [entity (world/mouseover-entity)] (:entity/id entity))))
    (add! #(str "elapsed-time " (utils.core/readable-number world/elapsed-time) " seconds"))
    (add! #(str "paused? " world/paused?))
    (add! #(str "GUI: " (g/gui-mouse-position)))
    (add! #(str "World: "(mapv int (g/world-mouse-position))))
    (add! #(str "Zoom: " (🎥/zoom (g/world-camera))))
    (add! #(str "logic-frame: " world/logic-frame))
    (add! fps)))

(defn- ->menu-bar []
  (let [menu-bar (MenuBar.)
        app-menu (Menu. "App")]
    (.addItem app-menu (menu-item "Map editor" (partial screen/change! :screens/map-editor)))
    (.addItem app-menu (menu-item "Properties" (partial screen/change! :screens/property-editor)))
    (.addItem app-menu (menu-item "Exit" app/exit!))
    (.addMenu menu-bar app-menu)
    (let [world (Menu. "World")]
      (doseq [{:keys [property/id]} (db/all :properties/worlds)]
        (.addItem world (menu-item (str "Start " id) (start-game-fn id))))
      (.addMenu menu-bar world))
    (let [help (Menu. "Help")]
        (.addItem help (MenuItem. "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[TAB] - Minimap\n[P]/[SPACE] - Unpause"))
      (.addMenu menu-bar help))
    (def mb menu-bar)
    (add-debug-infos mb)
    menu-bar))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (.getTable (->menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (a/set-touchable! :disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

(defn- world-actors []
  [(when dev-mode?
     (dev-menu))
   (ui/table {:rows [[{:actor (action-bar/create)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bars/create)
   (ui/group {:id :windows
              :actors [(entity-info-window/create)
                       (inventory/create)]})
   (ui/actor {:draw moon.creature.player.item-on-cursor/draw-item-on-cursor})
   (player-message/create)])
