(ns moon.screens.world
  (:require [data.grid2d :as g2d]
            [gdl.graphics :refer [frames-per-second delta-time]]
            [gdl.graphics.camera :as cam]
            [gdl.tiled :as tiled]
            [moon.controls :as controls]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.entity.movement :as movement]
            [moon.entity.player :as player]
            [moon.graphics.cursors :as cursors]
            [moon.graphics.world-view :as world-view]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.widgets.error-window :refer [error-window!]]
            [moon.widgets.windows :as windows]
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
         paused?
         ^{:doc "The game-logic frame number, starting with 1. (not counting when game is paused)"}
         logic-frame)

(defn start [world-id]
  (screen/change :screens/world)
  (bind-root #'logic-frame 0)
  (stage/reset (component/create [:world/widgets]))
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
    (component/->handle [[:tx/spawn-creatures level]])))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- player-state-pause-game? [] (entity/pause-game? (entity/state-obj @player/eid)))
(defn- player-update-state      [] (entity/manual-tick (entity/state-obj @player/eid)))

(defn- update-game-paused []
  (bind-root #'paused? (or tick-error
                           (and pausing?
                                (player-state-pause-game?)
                                (not (controls/unpaused?)))))
  nil)

(def ^:private update-world
  [player-update-state
   mouseover/update ; this do always so can get debug info even when game not running
   update-game-paused
   #(when-not paused?
      (alter-var-root #'logic-frame inc)
      (time/pass (min (delta-time) movement/max-delta-time))
      (let [entities (entities/active)]
        (update-potential-fields! entities)
        (try (entities/tick entities)
             (catch Throwable t
               (error-window! t)
               (bind-root #'tick-error t))))
      nil)
   [:tx/remove-destroyed-entities]]) ; do not pause this as for example pickup item, should be destroyed.

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
  (screen/enter [_])

  (screen/exit [_]
    (cursors/set :cursors/default))

  (screen/render [_]
    (render-world)
    (component/->handle update-world)
    (controls/world-camera-zoom)
    (windows/check-hotkeys)
    (cond (controls/close-windows?)
          (windows/close-all)

          (controls/minimap?)
          (screen/change :screens/minimap)))

  (screen/dispose [_]
    (tiled-map/clear)))

(defc :screens/world
  (component/create [_]
    (stage/create :screen (->WorldScreen))))
