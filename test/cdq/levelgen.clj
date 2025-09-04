(ns cdq.levelgen
  (:require [cdq.render.clear-screen]
            [cdq.textures-impl]
            [cdq.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.game.resize]
            [cdq.db-impl :as db]
            [cdq.level.modules]
            [cdq.level.uf-caves]
            [cdq.level.from-tmx]
            [cdq.gdx.tiled :as tiled]
            [cdq.input :as input]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.graphics.viewport :as viewport]
            [cdq.stage-impl :as stage]
            [cdq.gdx.backends.lwjgl :as lwjgl]
            [cdq.gdx.ui :as ui])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils Disposable)))

(def initial-level-fn [cdq.level.uf-caves/create {:tile-size 48
                                                  :texture-path "maps/uf_terrain.png"
                                                  :spawn-rate 0.02
                                                  :scaling 3
                                                  :cave-size 200
                                                  :cave-style :wide}])

(def level-fns
  [[#'cdq.level.from-tmx/create {:tmx-file "maps/vampire.tmx"
                                 :start-position [32 71]}]
   [#'cdq.level.uf-caves/create {:tile-size 48
                                 :texture-path "maps/uf_terrain.png"
                                 :spawn-rate 0.02
                                 :scaling 3
                                 :cave-size 200
                                 :cave-style :wide}]
   [#'cdq.level.modules/create {:world/map-size 5,
                                :world/max-area-level 3,
                                :world/spawn-rate 0.05}]])

(defn- show-whole-map! [{:keys [ctx/camera
                                ctx/tiled-map]}]
  (camera/set-position! camera
                        [(/ (:tiled-map/width  tiled-map) 2)
                         (/ (:tiled-map/height tiled-map) 2)])
  (camera/set-zoom! camera
                    (camera/calculate-zoom camera
                                           :left [0 0]
                                           :top [0 (:tiled-map/height tiled-map)]
                                           :right [(:tiled-map/width tiled-map) 0]
                                           :bottom [0 0])))

(def tile-size 48)



; when generating modules - I dispose right ? static tiled map tiles maybe not thrown?
;                java.lang.OutOfMemoryError: Java heap space
; com.badlogic.gdx.utils.GdxRuntimeException: java.lang.OutOfMemoryError: Java heap space

(defn- generate-level [{:keys [ctx/tiled-map] :as ctx} level-fn]
  (when tiled-map
    (Disposable/.dispose tiled-map))
  (let [level (let [[f params] level-fn]
                (f ctx params))
        tiled-map (:tiled-map level)
        ctx (assoc ctx :ctx/tiled-map tiled-map)]
    (tiled/set-visible! (tiled/get-layer tiled-map "creatures") true)
    (show-whole-map! ctx)
    ctx))

(def state (atom nil))

(defn- edit-window []
  (ui/window {:title "Edit"
              :cell-defaults {:pad 10}
              :rows (for [level-fn level-fns]
                      [(ui/text-button (str "Generate " (first level-fn))
                                       (fn [_actor _ctx]
                                         (swap! state (fn [ctx] (generate-level ctx level-fn)))))])
              :pack? true}))

(defrecord Context [])

(defn create! [gdx]
  (ui/load! {:skin-scale :x1})
  (let [input (:input gdx)
        ctx (map->Context {:ctx/input input})
        ui-viewport (viewport/fit 1440 900 (camera/orthographic))
        sprite-batch (SpriteBatch.)
        stage (ui/stage ui-viewport sprite-batch)
        _  (input/set-processor! input stage)
        tile-size 48
        world-unit-scale (float (/ tile-size))
        ctx (assoc ctx :ctx/stage stage)
        ctx (assoc ctx :ctx/db (db/create {:schemas "schema.edn"
                                           :properties "properties.edn"}))
        world-viewport (let [world-width  (* 1440 world-unit-scale)
                             world-height (* 900  world-unit-scale)]
                         (viewport/fit world-width
                                       world-height
                                       (camera/orthographic :y-down? false
                                                            :world-width world-width
                                                            :world-height world-height)))
        ctx (assoc ctx
                   :ctx/world-viewport world-viewport
                   :ctx/ui-viewport ui-viewport
                   :ctx/textures (cdq.textures-impl/create (:files gdx))
                   :ctx/camera (:viewport/camera world-viewport)
                   :ctx/color-setter (constantly [1 1 1 1])
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/sprite-batch sprite-batch
                   :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale sprite-batch))
        ctx (generate-level ctx initial-level-fn)]
    (stage/add! (:ctx/stage ctx) (edit-window))
    (reset! state ctx)))

(defn dispose! []
  ; TODO ? disposing properly everything cdq.start stuff??
  ; batch, cursors, default-font, shape-drawer-texture, etc.
  (ui/dispose!)
  (let [{:keys [ctx/sprite-batch
                ctx/tiled-map]} @state]
    (Disposable/.dispose sprite-batch) ; TODO that wont work anymore -> and one more fn so have to move it together?
    (Disposable/.dispose tiled-map)))

(defn- draw-tiled-map! [{:keys [ctx/color-setter
                                ctx/tiled-map
                                ctx/tiled-map-renderer
                                ctx/world-viewport]}]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     tiled-map
                     color-setter))

(defn- camera-movement-controls! [{:keys [ctx/input
                                          ctx/camera
                                          ctx/camera-movement-speed]}]
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (:camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (input/key-pressed? input :left)  (apply-position 0 -))
    (if (input/key-pressed? input :right) (apply-position 0 +))
    (if (input/key-pressed? input :up)    (apply-position 1 +))
    (if (input/key-pressed? input :down)  (apply-position 1 -))))

(defn- camera-zoom-controls! [{:keys [ctx/input
                                      ctx/camera
                                      ctx/zoom-speed]}]
  (when (input/key-pressed? input :minus)  (camera/inc-zoom! camera zoom-speed))
  (when (input/key-pressed? input :equals) (camera/inc-zoom! camera (- zoom-speed))))

(defn- render-stage! [{:keys [ctx/stage]
                       :as ctx}]
  (stage/render! stage ctx))

(defn render! []
  (cdq.render.clear-screen/do! @state)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state)
  (camera-movement-controls! @state)
  (render-stage! @state))

(defn resize! [width height]
  (cdq.game.resize/do! @state width height))

(defn -main []
  (lwjgl/start-application! {:title "Levelgen test"
                             :windowed-mode {:width 1440 :height 900}
                             :foreground-fps 60}
                            {:create! create!
                             :dispose! dispose!
                             :render! render!
                             :resize! resize!}))
