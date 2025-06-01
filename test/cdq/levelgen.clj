(ns cdq.levelgen
  (:require [cdq.create.assets]
            [cdq.create.db]
            [cdq.level.modules :as modules]
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.input :as input]
            [clojure.gdx.utils.disposable :as disposable]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.tiled-map-renderer :as tm-renderer]
            [gdl.tiled :as tiled]
            [gdl.viewport :as viewport]))

(defn- show-whole-map! [camera tiled-map]
  (camera/set-position! camera
                        [(/ (tiled/tm-width  tiled-map) 2)
                         (/ (tiled/tm-height tiled-map) 2)])
  (camera/set-zoom! camera
                    (camera/calculate-zoom camera
                                           :left [0 0]
                                           :top [0 (tiled/tm-height tiled-map)]
                                           :right [(tiled/tm-width tiled-map) 0]
                                           :bottom [0 0])))

(def tile-size 48)

(defrecord Context [])

(def state (atom nil))

(defn create! [config]
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config {:db {:schemas "schema.edn"
                                         :properties "properties.edn"}
                                    :assets {:folder "resources/"
                                             :asset-type-extensions {:texture #{"png" "bmp"}}}})
        ctx (cdq.create.db/do!     ctx)
        ctx (cdq.create.assets/do! ctx)
        level (modules/create ctx)
        world-unit-scale (float (/ tile-size))
        world-viewport (graphics/world-viewport world-unit-scale
                                                {:width 1440
                                                 :height 900})
        tiled-map (:tiled-map level)
        batch (graphics/sprite-batch)
        tm-renderer (tm-renderer/create tiled-map
                                        world-unit-scale
                                        batch)
        ctx (assoc ctx
                   :ctx/input (gdx/input)
                   :ctx/tm-renderer tm-renderer
                   :ctx/tiled-map tiled-map
                   :ctx/world-viewport world-viewport
                   :ctx/camera (:camera world-viewport)
                   :ctx/color-setter (constantly color/white)
                   :ctx/zoom-speed 0.1)]
    (show-whole-map! (:camera world-viewport) tiled-map)
    (reset! state ctx)
    (println level)))

(defn dispose! []
  (let [{:keys [ctx/assets
                ctx/tiled-map]} @state]
    (disposable/dispose! assets)
    (disposable/dispose! tiled-map)))

(defn- draw-tiled-map! [{:keys [ctx/tm-renderer
                                ctx/tiled-map
                                ctx/camera
                                ctx/color-setter]}]
  (tm-renderer/draw! tm-renderer
                     tiled-map
                     color-setter
                     camera))

(defn- camera-zoom-controls! [{:keys [ctx/input
                                      ctx/camera
                                      ctx/zoom-speed]}]
  (when (input/key-pressed? input :minus)  (camera/inc-zoom! camera zoom-speed))
  (when (input/key-pressed? input :equals) (camera/inc-zoom! camera (- zoom-speed))))

(defn render! []
  (graphics/clear-screen! color/black)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state))

(defn resize! [width height]
  (let [{:keys [ctx/world-viewport]} @state]
    (viewport/resize! world-viewport width height)))
