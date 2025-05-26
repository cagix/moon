(ns gdl.application
  (:require [clojure.gdx.graphics.camera :as camera]
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
