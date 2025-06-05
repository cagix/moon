(ns cdq.levelgen
  (:require cdq.schemas-impl ; invalid schema :s/sound when reading db.
            [cdq.level.modules]
            [cdq.level.uf-caves]
            [cdq.level.vampire]
            [cdq.create.gdx]
            [cdq.create.db]
            [cdq.graphics.tiled-map-renderer :as tm-renderer]
            [cdq.render.clear-screen]
            [cdq.utils.camera :as camera-utils]
            [clojure.gdx :as gdx]
            [clojure.gdx.ui :as ui]
            [clojure.graphics.camera :as camera]
            [clojure.graphics :as graphics]
            [clojure.graphics.viewport :as viewport]
            [clojure.input :as input]
            [clojure.tiled :as tiled]
            [clojure.utils.disposable :as disp]))

(defn- show-whole-map! [{:keys [ctx/camera
                                ctx/tiled-map]}]
  (camera/set-position! camera
                        [(/ (:tiled-map/width  tiled-map) 2)
                         (/ (:tiled-map/height tiled-map) 2)])
  (camera/set-zoom! camera
                    (camera-utils/calculate-zoom camera
                                                 :left [0 0]
                                                 :top [0 (:tiled-map/height tiled-map)]
                                                 :right [(:tiled-map/width tiled-map) 0]
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
    (disp/dispose! tiled-map))
  (let [level (level-fn ctx)
        tiled-map (:tiled-map level)
        ctx (assoc ctx
                   :ctx/tm-renderer (tm-renderer/create tiled-map world-unit-scale batch)
                   :ctx/tiled-map tiled-map)]
    (tiled/set-visible! (tiled/get-layer tiled-map "creatures") true)
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

(defn create! [_config]
  (ui/load! {:skin-scale :x1})
  (let [ctx (->Context)
        ctx (cdq.create.gdx/do! ctx
                                {:assets {:folder "resources/"
                                          :asset-type-extensions {:sound   #{"wav"}
                                                                  :texture #{"png" "bmp"}}}
                                 :tile-size 48
                                 :ui-viewport {:width 1440
                                               :height 900}
                                 :world-viewport {:width 1440
                                                  :height 900}
                                 })
        ctx (assoc ctx :ctx/db (cdq.create.db/do!     ctx {:schemas "schema.edn"
                                                           :properties "properties.edn"}))
        batch (gdx/sprite-batch)
        stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        batch)
        ctx (assoc ctx
                   :ctx/batch batch
                   :ctx/camera (:camera (:ctx/world-viewport ctx))
                   :ctx/color-setter (constantly (gdx/->color :white))
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/stage stage)
        ctx (generate-level ctx cdq.level.modules/create)]
    (input/set-processor! (:ctx/input ctx) stage)
    (ui/add! stage (edit-window))
    (reset! state ctx)))

(defn dispose! []
  (let [{:keys [ctx/assets
                ctx/tiled-map]} @state]
    (disp/dispose! assets)
    (disp/dispose! tiled-map)))

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
  (cdq.render.clear-screen/do! @state)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state)
  (camera-movement-controls! @state)
  (render-stage! @state))

(defn resize! [width height]
  (let [{:keys [ctx/ui-viewport
                ctx/world-viewport]} @state]
    (viewport/resize! ui-viewport    width height)
    (viewport/resize! world-viewport width height)))
