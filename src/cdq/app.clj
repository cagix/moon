(ns cdq.app
  (:require [clojure.application :as application]
            [clojure.edn :as edn]
            [clojure.files :as files]
            [clojure.files.search :as file-search]
            [clojure.gdx.backends.lwjgl3.application :as lwjgl3]
            [clojure.gdx :as gdx]
            [clojure.gdx.assets.manager :as asset-manager]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.vis-ui :as vis-ui]
            [clojure.graphics :as graphics]
            [clojure.input :as input]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.utils :refer [mapvals safe-merge tile->middle]]
            [clojure.utils.disposable :refer [dispose]]
            [gdl.context :as gdl.context]
            [gdl.db :as db]
            [gdl.ui :as ui]
            [anvil.level :refer [generate-level]]
            [cdq.context :refer [spawn-creature]]
            [cdq.context.stage-actors :as stage-actors]
            [cdq.context.explored-tile-corners :as explored-tile-corners]
            [cdq.context.content-grid :as content-grid]
            [cdq.context.grid :as grid]
            [cdq.context.raycaster :as raycaster]

            gdl.context
            gdl.graphics
            cdq.context
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

(defn- create-cursors [{:keys [clojure.gdx/files
                               clojure.gdx/graphics]} cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (gdx/pixmap (files/internal files (str "cursors/" file ".png")))
                   cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
               (dispose pixmap)
               cursor))
           cursors))

(defn- cached-tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- world-viewport [{:keys [width height]} world-unit-scale]
  (println "World-viewport: " width ", " height ", world-unit-scale " world-unit-scale)
  (assert world-unit-scale)
  (let [camera (gdx/orthographic-camera)
        world-width  (* width  world-unit-scale)
        world-height (* height world-unit-scale)]
    (camera/set-to-ortho camera world-width world-height :y-down? false)
    (gdx/fit-viewport world-width world-height camera)))

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
  (vis-ui/load (:vis-ui config))
  (let [batch (sprite-batch/create)
        ; => pixmap namespace
        sd-texture (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                                  (pixmap/set-color gdx/white)
                                  (gdx/draw-pixel 0 0))
                         texture (gdx/texture pixmap)]
                     (dispose pixmap)
                     texture)
        ; => fit-viewport namespace
        ui-viewport (gdx/fit-viewport (:width  (:ui-viewport config))
                                      (:height (:ui-viewport config))
                                      ; => orthographic-camera namespace
                                      (gdx/orthographic-camera))
        world-unit-scale (float (/ (:tile-size config)))
        stage (ui/stage ui-viewport batch nil)
        _ (input/set-processor input stage)
        context (safe-merge context
                            {:gdl.context/assets (asset-manager/create
                                                  (search-assets files (:assets config)))
                             :gdl.context/batch batch
                             :gdl.context/cursors (create-cursors context (:cursors config))
                             :gdl.context/db (db/create (:db config))
                             :gdl.context/default-font (freetype/generate-font (update (:default-font config) :file #(files/internal files %)))
                             :gdl.context/shape-drawer (sd/create batch (gdx/texture-region sd-texture 1 0 1 1))
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
  (vis-ui/dispose)
  ; TODO dispose :gdl.context/sd-texture
  (dispose (:gdl.context/assets context))
  (dispose (:gdl.context/batch  context))
  (run! dispose (vals (:gdl.context/cursors context)))
  (dispose (:gdl.context/default-font context))
  (dispose (:gdl.context/stage context))
  (dispose (:cdq.context/tiled-map context))  ; TODO ! this also if world restarts !!
  )

(defn- resize [context width height]
  (gdx/resize (:gdl.context/viewport       context) width height :center-camera? true)
  (gdx/resize (:gdl.context/world-viewport context) width height :center-camera? false))

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


(def state (atom nil))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (lwjgl3/create (reify application/Listener
                     (create [_ context]
                       (reset! state (create context (:context config))))

                     (dispose [_]
                       (dispose! @state))

                     (pause [_])

                     (render [_]
                       (swap! state render))

                     (resize [_ width height]
                       (resize @state width height))

                     (resume [_]))
                   (:app config))))
