(ns gdl.application
  (:require [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.interop :as interop]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.tiled :as tiled]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils Disposable
                                   ScreenUtils)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defmulti ^:private draw! (fn [[k] _ctx]
                            k))

(defmethod draw! :draw/image [[_ sprite position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (graphics/draw-sprite! batch
                         unit-scale
                         sprite
                         position))

(defmethod draw! :draw/rotated-centered [[_ sprite rotation position]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (graphics/draw-sprite! batch
                         unit-scale
                         sprite
                         position
                         rotation))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [ctx/default-font
                                     ctx/batch
                                     ctx/unit-scale]}]
  (bitmap-font/draw! (or font default-font)
                     batch
                     {:scale (* (float unit-scale)
                                (float (or scale 1)))
                      :x x
                      :y y
                      :text text
                      :h-align h-align
                      :up? up?}))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] ctx]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] ctx))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] ctx))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [ctx/shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (c/handle-draws! ctx draws))))

(defprotocol Viewports
  (update-viewports! [_]))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/unit-scale
                      ctx/world-unit-scale
                      ctx/shape-drawer-texture
                      ctx/shape-drawer
                      ctx/cursors
                      ctx/default-font
                      ctx/world-viewport
                      ctx/ui-viewport
                      ctx/tiled-map-renderer
                      ctx/stage]
  Disposable
  (dispose [_] ; TODO !
    (Disposable/.dispose assets)
    (Disposable/.dispose batch)
    (Disposable/.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals cursors))
    (Disposable/.dispose default-font)
    ; TODO vis-ui dispose
    )

  Viewports
  (update-viewports! [_]
    (viewport/update! ui-viewport)
    (viewport/update! world-viewport))

  c/Graphics
  (delta-time [_]
    (.getDeltaTime Gdx/graphics))

  (set-cursor! [_ cursor-key]
    (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))

  (frames-per-second [_]
    (.getFramesPerSecond Gdx/graphics))

  (clear-screen! [_]
    (ScreenUtils/clear Color/BLACK))

  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (draw-on-world-viewport! [ctx fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (draw-tiled-map! [_ tiled-map color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (world-mouse-position [_]
    (viewport/mouse-position world-viewport))

  (ui-mouse-position [_]
    (viewport/mouse-position ui-viewport))

  (ui-viewport-width [_]
    (:width ui-viewport))

  (ui-viewport-height [_]
    (:height ui-viewport))

  (world-viewport-width [_]
    (:width world-viewport))

  (world-viewport-height [_]
    (:height world-viewport))

  (camera-position [_]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [_ amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [_]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [_]
    (camera/visible-tiles (:camera world-viewport)))

  (camera-zoom [_]
    (camera/zoom (:camera world-viewport)))

  (pixels->world-units [_ pixels]
    (* pixels world-unit-scale))

  (sprite [this texture-path]
    (graphics/sprite (graphics/texture-region (c/texture this texture-path))
                     world-unit-scale))

  (sub-sprite [_ sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [this texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (c/texture this texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite this image [(* x tilew) (* y tileh) tilew tileh])))

(defn create-state! [config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (viewport/ui-viewport (:ui-viewport config))]
    (map->Context {:batch batch
                   :unit-scale 1
                   :world-unit-scale world-unit-scale
                   :shape-drawer-texture shape-drawer-texture
                   :shape-drawer (sd/create (:java-object batch)
                                            (graphics/texture-region shape-drawer-texture 1 0 1 1))
                   :cursors (utils/mapvals
                             (fn [[file [hotspot-x hotspot-y]]]
                               (graphics/create-cursor (format (:cursor-path-format config) file)
                                                       hotspot-x
                                                       hotspot-y))
                             (:cursors config))
                   :default-font (graphics/truetype-font (:default-font config))
                   :world-viewport (viewport/world-viewport world-unit-scale
                                                            (:world-viewport config))
                   :ui-viewport ui-viewport
                   :tiled-map-renderer (memoize (fn [tiled-map]
                                                  (tiled/renderer tiled-map
                                                                  world-unit-scale
                                                                  (:java-object batch))))})))
