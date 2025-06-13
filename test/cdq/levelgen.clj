(ns cdq.levelgen
  (:require [cdq.create.db]
            [cdq.level.modules]
            [cdq.level.uf-caves]
            [cdq.level.vampire]
            [cdq.render.clear-screen]
            [cdq.utils.camera :as camera-utils]
            gdl.create.graphics
            gdl.create.ui
            [gdl.graphics.camera :as camera]
            [gdl.graphics :as graphics]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]
            [gdl.utils.disposable :as disp]
            [gdx.tiled :as tiled]))

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

(defn- generate-level [{:keys [ctx/tiled-map] :as ctx} level-fn]
  (when tiled-map
    (disp/dispose! tiled-map))
  (let [level (level-fn ctx)
        tiled-map (:tiled-map level)
        ctx (assoc ctx :ctx/tiled-map tiled-map)]
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

(defn create! [context _params]
  (let [ctx (merge (->Context) context)
        ctx (assoc ctx :ctx/graphics (gdl.create.graphics/do! ctx {:textures {:folder "resources/"
                                                                              :extensions #{"png" "bmp"}}
                                                                   :tile-size 48
                                                                   :ui-viewport {:width 1440
                                                                                 :height 900}
                                                                   :world-viewport {:width 1440
                                                                                    :height 900}}))
        ctx (assoc ctx :ctx/stage (gdl.create.ui/do! ctx {:skin-scale :x1}))
        ctx (assoc ctx :ctx/db (cdq.create.db/do!     ctx {:schemas "schema.edn"
                                                           :properties "properties.edn"}))
        ctx (assoc ctx
                   :ctx/camera (:camera (:world-viewport (:ctx/graphics ctx)))
                   :ctx/color-setter (constantly [1 1 1 1])
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1)
        ctx (generate-level ctx cdq.level.modules/create)]
    (stage/add! (:ctx/stage ctx) (edit-window))
    (reset! state ctx)))

(defn dispose! []
  ; TODO ? disposing properly everything gdl.start stuff??
  ; batch, cursors, default-font, shape-drawer-texture, etc.
  (let [{:keys [ctx/graphics
                ctx/tiled-map]} @state]
    (disp/dispose! graphics)
    (disp/dispose! tiled-map)))

(defn- draw-tiled-map! [{:keys [ctx/graphics
                                ctx/tiled-map
                                ctx/color-setter]}]
  (graphics/draw-tiled-map! graphics tiled-map color-setter))

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
  (stage/render! stage ctx))

(defn render! [_]
  (cdq.render.clear-screen/do! @state)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state)
  (camera-movement-controls! @state)
  (render-stage! @state))

(defn resize! [width height]
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
