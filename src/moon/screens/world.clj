(ns ^:no-doc moon.screens.world
  (:require [gdl.graphics :refer [clear-screen frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.input :refer [key-pressed? key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.ui.stage]
            [gdl.utils :refer [dev-mode?]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.widgets.error-window :refer [error-window!]]
            [moon.world :as world]
            [moon.world.debug-render :as debug-render]
            [moon.world.potential-fields :refer [update-potential-fields!]]))

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

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- player-state-pause-game? [] (entity/pause-game? (entity/state-obj @world/player)))
(defn- player-update-state      [] (entity/manual-tick (entity/state-obj @world/player)))

(defn- player-unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space))) ; FIXMe :keys? shouldnt it be just :space?

(defn- update-game-paused []
  (.bindRoot #'world/paused? (or world/entity-tick-error
                                 (and pausing?
                                      (player-state-pause-game?)
                                      (not (player-unpaused?)))))
  nil)

(deftype WorldScreen []
  screen/Screen
  (screen/enter! [_])

  (screen/exit! [_]
    (g/set-cursor! :cursors/default))

  (screen/render! [_]
    (clear-screen :black)
    ; FIXME position DRY
    (cam/set-position! (g/world-camera) (:position @world/player))
    ; FIXME position DRY
    (world/render-tiled-map! (cam/position (g/world-camera)))
    (g/render-world-view! (fn []
                            (debug-render/before-entities)
                            ; FIXME position DRY (from player)
                            (world/render-entities! (map deref (world/active-entities)))
                            (debug-render/after-entities)))
    (component/->handle
     [player-update-state
      ; this do always so can get debug info even when game not running
      world/update-mouseover-entity!
      update-game-paused
      #(when-not world/paused?
         (alter-var-root #'world/logic-frame inc)
         (let [delta (min (delta-time) entity/max-delta-time)]
           (.bindRoot      #'world/delta-time delta)
           (alter-var-root #'world/elapsed-time + delta))
         (let [entities (world/active-entities)]
           (update-potential-fields! entities)
           (try (run! world/tick-system entities)
                (catch Throwable t
                  (error-window! t)
                  (.bindRoot #'world/entity-tick-error t))))
         nil)
      ; do not pause this as for example pickup item, should be destroyed.
      [:tx/remove-destroyed-entities]])
    (check-key-input))

  (screen/dispose! [_]
    (world/clear-tiled-map)))

(defc :screens/world
  (component/create [_]
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
   (component/create [:widgets/draw-item-on-cursor nil])
   (component/create [:widgets/player-message      nil])])

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
