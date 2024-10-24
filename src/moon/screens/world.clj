(ns ^:no-doc moon.screens.world
  (:require [gdl.graphics :refer [clear-screen frames-per-second]]
            [gdl.graphics.camera :as cam]
            [gdl.input :refer [key-pressed? key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.ui.stage]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.graphics :as g]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            moon.creature.player.item-on-cursor
            [moon.world :as world]))

(defn- check-window-hotkeys []
  (doseq [[hotkey window-id] {:keys/i :inventory-window
                              :keys/e :entity-info-window}
          :when (key-just-pressed? hotkey)]
    (a/toggle-visible! (get (:windows (stage/get)) window-id))))

(defn- close-windows?! []
  (let [windows (ui/children (:windows (stage/get)))]
    (if (some a/visible? windows)
      (do
       (run! #(a/set-visible! % false) windows)
       true))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (cam/set-zoom! camera (max 0.1 (+ (cam/zoom camera) by))))

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
    (clear-screen :black)
    (world/tick!)
    (check-key-input))

  (screen/dispose! [_]
    (world/clear-tiled-map)))

(defc :screens/world
  (screen/create [_]
    (stage/create :screen (->WorldScreen))))

(defn- world-actors []
  [(if dev-mode?
     (component/create [:widgets/dev-menu nil])
     (ui/actor {}))
   (ui/table {:rows [[{:actor (component/create [:widgets/action-bar nil])
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (component/create [:widgets/hp-mana nil])
   (ui/group {:id :windows
              :actors [(component/create [:widgets/entity-info-window nil])
                       (component/create [:widgets/inventory          nil])]})
   (ui/actor {:draw moon.creature.player.item-on-cursor/draw-item-on-cursor})
   (component/create [:widgets/player-message nil])])

(defn- reset-stage! []
  (let [stage (stage/get)]
    (gdl.ui.stage/clear! stage)
    (run! #(gdl.ui.stage/add! stage %) (world-actors))))

(.bindRoot #'world/start
           (fn start-game-fn [world-id]
             (screen/change! :screens/world)
             (reset-stage!)
             (let [level (level/generate world-id)]
               (world/init! (:tiled-map level))
               (world/spawn-entities level))))
