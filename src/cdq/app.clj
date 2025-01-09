(ns cdq.app
  (:require [clojure.files :as files]
            [clojure.files.search :as file-search]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.input :as input]
            [clojure.string :as str]
            [clojure.utils :refer [mapvals safe-merge tile->middle]]
            [clojure.utils.disposable :refer [dispose]]
            [gdl.app :as app]
            [gdl.assets :as assets]
            [gdl.context :as gdl.context]
            [gdl.ui :as ui]
            [cdq.db :as db]
            [gdl.graphics]
            [gdl.ui :as ui]
            [cdq.level :refer [generate-level]]
            [cdq.context :refer [spawn-creature]]
            [cdq.context.stage-actors :as stage-actors]
            [cdq.context.explored-tile-corners :as explored-tile-corners]
            [cdq.context.content-grid :as content-grid]
            [cdq.context.grid :as grid]
            [cdq.context.raycaster :as raycaster]
            gdl.graphics
            cdq.graphics
            cdq.graphics.camera
            cdq.graphics.tiled-map)
  (:import (gdl OrthogonalTiledMapRenderer))
  (:gen-class))

(defn- search-assets [files folder]
  (for [[asset-type exts] {:sound   #{"wav"}
                           :texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (file-search/by-extensions (files/internal files folder)
                                             exts))]
    [file asset-type]))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (println "World-viewport: " width ", " height ", world-unit-scale " world-unit-scale)
  (assert world-unit-scale)
  (let [camera (orthographic-camera/create)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (fit-viewport/create world-width world-height camera)))

; TODO this passing w. world props ...
; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-player-entity [context start-position]
  (spawn-creature context (player-entity-props start-position)))

(defn- spawn-enemies! [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle))))

; * application context independent of game/level restarts or changes
; * application context dependent on game/level restarts
; * => multimethod
; * => GameContext -> dispose,resize, and later can add 'restart-level, restart-game'....

(defn- create [{:keys [clojure.gdx/files
                       clojure.gdx/input]
                :as context} config]
  (let [ui-viewport (fit-viewport/create (:width  (:ui-viewport config))
                                         (:height (:ui-viewport config))
                                         (orthographic-camera/create))
        world-unit-scale (float (/ (:tile-size config)))

        gdl-graphics (gdl.graphics/create context (:graphics config))
        batch (:batch gdl-graphics)
        shape-drawer (:sd gdl-graphics)
        sd-texture (:sd-texture gdl-graphics)
        cursors (:cursors gdl-graphics)

        _ (ui/load! (:ui config))
        stage (ui/stage ui-viewport batch nil)
        _ (input/set-processor input stage)

        context (safe-merge context
                            {:gdl.context/assets (assets/create (search-assets files (:assets config)))
                             :gdl.context/batch batch
                             :gdl.context/cursors cursors
                             :gdl.context/db (db/create (:db config))
                             :gdl.context/default-font (freetype/generate-font (update (:default-font config) :file #(files/internal files %)))
                             :gdl.context/shape-drawer shape-drawer
                             :gdl.context/sd-texture sd-texture

                             :gdl.context/stage stage
                             :gdl.context/viewport ui-viewport
                             :gdl.context/world-viewport (world-viewport (:world-viewport config) world-unit-scale)
                             :gdl.context/world-unit-scale world-unit-scale
                             :gdl.context/tiled-map-renderer (cached-tiled-map-renderer batch world-unit-scale)
                             ;; - before here - application context - does not change on level/game restart -
                             :gdl.context/elapsed-time 0
                             :cdq.context/player-message (atom (:player-message config))})]
    (gdl.context/reset-stage context
                             (stage-actors/create context)) ; TODO this is not part of context keys anymore, can't be dispatched ... on actors
    ; so it should be part of stage/ui then? for resett-ing later ?
    (let [level (generate-level context
                                (gdl.context/build context (:world-id config)))
          tiled-map (:tiled-map level)
          grid (grid/create tiled-map)
          context (safe-merge context
                              {:cdq.context/error nil
                               :cdq.context/level level
                               :cdq.context/tiled-map tiled-map
                               :cdq.context/grid grid
                               :cdq.context/explored-tile-corners (explored-tile-corners/create tiled-map)
                               :cdq.context/content-grid (content-grid/create tiled-map (:content-grid config))
                               :cdq.context/entity-ids (atom {})
                               :cdq.context/raycaster (raycaster/create grid)
                               :cdq.context/factions-iterations (:factions-iterations config)})
          context (assoc context :cdq.context/player-eid (spawn-player-entity context (:start-position level)))]
      (spawn-enemies! context tiled-map)
      context)))

(defn- dispose! [context]
  ; TODO dispose :gdl.context/sd-texture
  (dispose (:gdl.context/assets context))
  (dispose (:gdl.context/batch  context))
  (run! dispose (vals (:gdl.context/cursors context)))
  (dispose (:gdl.context/default-font context))

  (ui/dispose!)
  (dispose (:gdl.context/stage context))

  (dispose (:cdq.context/tiled-map context))  ; TODO ! this also if world restarts !!
  )

(defn- resize [context width height]
  (viewport/resize (:gdl.context/viewport       context) width height :center-camera? true)
  (viewport/resize (:gdl.context/world-viewport context) width height :center-camera? false))

(defn- render [context]
  (reduce (fn [context f]
            (f context))
          context
          [gdl.graphics/clear-screen
           cdq.graphics.camera/set-on-player-position
           cdq.graphics.tiled-map/render
           cdq.graphics/draw-world-view
           gdl.graphics/draw-stage

           ; updates
           gdl.context/update-stage
           cdq.context/handle-player-input
           cdq.context/update-mouseover-entity
           cdq.context/update-paused-state
           cdq.context/progress-time-if-not-paused
           cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
           gdl.context/check-camera-controls
           cdq.context/check-ui-key-listeners]))


(defn -main []
  (app/start "app.edn"
             create
             dispose!
             render
             resize))
