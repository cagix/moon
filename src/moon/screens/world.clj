(ns moon.screens.world
  (:require [gdl.graphics :refer [frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.input :refer [key-pressed? key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.entity :as entity]
            [moon.graphics.cursors :as cursors]
            [moon.graphics.world-view :as world-view]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.widgets.error-window :refer [error-window!]]
            [moon.world :as world]
            [moon.world.debug-render :as debug-render]
            [moon.world.entities :as entities]
            [moon.world.potential-fields :refer [update-potential-fields!]]
            [moon.world.tiled-map :refer [render-tiled-map]]
            [moon.world.time :as world.time]))

(defn- check-window-hotkeys []
  (doseq [[hotkey window-id] {:keys/i :inventory-window
                              :keys/e :entity-info-window}
          :when (key-just-pressed? hotkey)]
    (a/toggle-visible! (get (:windows (stage/get)) window-id))))

(defn- close-windows []
  (let [windows (ui/children (:windows (stage/get)))]
    (when (some a/visible? windows)
      (run! #(a/set-visible! % false) windows))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (cam/set-zoom! camera (max 0.1 (+ (cam/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- check-zoom-keys []
  (let [camera (world-view/camera)]
    (when (key-pressed? :keys/minus)  (adjust-zoom camera    zoom-speed))
    (when (key-pressed? :keys/equals) (adjust-zoom camera (- zoom-speed)))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- player-state-pause-game? [] (entity/pause-game? (entity/state-obj @world/player)))
(defn- player-update-state      [] (entity/manual-tick (entity/state-obj @world/player)))

(defn- player-unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space))) ; FIXMe :keys? shouldnt it be just :space?

(defn- update-game-paused []
  (bind-root #'world/paused? (or world/entity-tick-error
                                 (and pausing?
                                      (player-state-pause-game?)
                                      (not (player-unpaused?)))))
  nil)

(defn- update-time []
  (alter-var-root #'world/logic-frame inc)
  (let [delta (min (delta-time) entity/max-delta-time)]
    (bind-root      #'world/delta-time delta)
    (alter-var-root #'world.time/elapsed + delta)))

(def ^:private update-world
  [player-update-state
   entities/update-mouseover ; this do always so can get debug info even when game not running
   update-game-paused
   #(when-not world/paused?
      (update-time)
      (let [entities (world/active-entities)]
        (update-potential-fields! entities)
        (try (entities/tick entities)
             (catch Throwable t
               (error-window! t)
               (bind-root #'world/entity-tick-error t))))
      nil)
   [:tx/remove-destroyed-entities]]) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-view/camera) (:position @world/player))
  ; FIXME position DRY
  (render-tiled-map (cam/position (world-view/camera)))
  (world-view/render (fn []
                       (debug-render/before-entities)
                       ; FIXME position DRY (from player)
                       (entities/render (map deref (world/active-entities)))
                       (debug-render/after-entities))))

(deftype WorldScreen []
  screen/Screen
  (screen/enter [_])

  (screen/exit [_]
    (cursors/set :cursors/default))

  (screen/render [_]
    (render-world)
    (component/->handle update-world)
    (check-zoom-keys)
    (check-window-hotkeys)
    (cond (key-just-pressed? :keys/escape)
          (close-windows)

          (key-just-pressed? :keys/tab)
          (screen/change :screens/minimap)))

  (screen/dispose [_]
    (world/clear-tiled-map)))

(defc :screens/world
  (component/create [_]
    (stage/create :screen (->WorldScreen))))
