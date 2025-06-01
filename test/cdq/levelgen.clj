(ns cdq.levelgen
  (:require [cdq.create.db]
            [cdq.level.modules]
            [cdq.level.uf-caves]
            [cdq.level.vampire]
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.input :as input]
            [clojure.gdx.utils.disposable :as disposable]
            [gdl.create.assets]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.tiled-map-renderer :as tm-renderer]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.viewport :as viewport]))

(defn- show-whole-map! [{:keys [ctx/camera
                                ctx/tiled-map]}]
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



; when generating modules - I dispose right ? static tiled map tiles maybe not thrown?
;                java.lang.OutOfMemoryError: Java heap space
; com.badlogic.gdx.utils.GdxRuntimeException: java.lang.OutOfMemoryError: Java heap space

(defn- generate-level [{:keys [ctx/tiled-map
                               ctx/batch
                               ctx/world-unit-scale]
                        :as ctx}
                       level-fn]
  (when tiled-map
    (disposable/dispose! tiled-map))
  (let [level (level-fn ctx)
        tiled-map (:tiled-map level)
        ctx (assoc ctx
                   :ctx/tm-renderer (tm-renderer/create tiled-map world-unit-scale batch)
                   :ctx/tiled-map tiled-map)]
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)
    (show-whole-map! ctx)
    ctx))

(def state (atom nil))

(defn- edit-window []
  (ui/window {:title "Edit"
              :cell-defaults {:pad 10}
              :rows (for [level-fn [#'cdq.level.modules/create
                                    #'cdq.level.uf-caves/create
                                    #'cdq.level.vampire/create]]
                      [(ui/text-button (str "Generate " level-fn)
                                       (fn [_actor _ctx]
                                         (swap! state generate-level level-fn)))])
              :pack? true}))

(defrecord Context [])

(defn create! [config]
  (ui/load! {:skin-scale :x1})
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config {:db {:schemas "schema.edn"
                                         :properties "properties.edn"}
                                    :assets {:folder "resources/"
                                             :asset-type-extensions {:texture #{"png" "bmp"}}}})
        ctx (cdq.create.db/do!     ctx)
        ctx (gdl.create.assets/do! ctx)
        world-unit-scale (float (/ tile-size))
        world-viewport (graphics/world-viewport world-unit-scale
                                                {:width 1440
                                                 :height 900})
        batch (graphics/sprite-batch)
        ui-viewport (graphics/ui-viewport {:width 1440
                                           :height 900})
        stage (ui/stage (:java-object ui-viewport)
                        batch)
        input (gdx/input)
        ctx (assoc ctx
                   :ctx/batch batch
                   :ctx/input input
                   :ctx/world-unit-scale world-unit-scale
                   :ctx/world-viewport world-viewport
                   :ctx/camera (:camera world-viewport)
                   :ctx/color-setter (constantly color/white)
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/stage stage
                   :ctx/ui-viewport ui-viewport)
        ctx (generate-level ctx cdq.level.modules/create)]
    (input/set-processor! input stage)
    (ui/add! stage (edit-window))
    (reset! state ctx)))

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

(defn- camera-movement-controls! [{:keys [ctx/input
                                          ctx/camera
                                          ctx/camera-movement-speed]}]
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (camera/position camera)
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
  (ui/act!  stage ctx)
  (ui/draw! stage ctx))

(defn render! []
  (graphics/clear-screen! color/black)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state)
  (camera-movement-controls! @state)
  (render-stage! @state))

(defn resize! [width height]
  (let [{:keys [ctx/ui-viewport
                ctx/world-viewport]} @state]
    (viewport/resize! ui-viewport    width height)
    (viewport/resize! world-viewport width height)))
