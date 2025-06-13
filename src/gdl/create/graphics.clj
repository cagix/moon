(ns gdl.create.graphics
  (:require [gdl.files :as files]
            [gdl.graphics]
            [gdl.graphics.camera]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region]
            [gdl.graphics.viewport]
            [gdl.utils.assets :as assets]
            [gdl.utils.disposable]
            [gdx.graphics :as graphics]
            [gdx.graphics.color :as color]
            [gdx.graphics.colors :as colors]
            [gdx.graphics.g2d :as g2d]
            [gdx.graphics.g2d.freetype :as freetype]
            [gdx.graphics.shape-drawer :as sd]
            [gdx.math.vector3 :as vector3]
            [gdx.tiled :as tiled]
            [gdx.utils.screen :as screen-utils]
            [gdx.utils.viewport :as viewport])
  (:import (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn- create-cursors [graphics files cursors cursor-path-format]
  (update-vals cursors
               (fn [[file hotspot]]
                 (graphics/create-cursor graphics
                                         (files/internal files (format cursor-path-format file))
                                         hotspot))))

(defrecord Graphics [
                     batch
                     cursors
                     default-font
                     graphics
                     shape-drawer-texture
                     shape-drawer
                     textures
                     tiled-map-renderer
                     ui-viewport
                     unit-scale
                     world-unit-scale
                     world-viewport
                     ]
  gdl.utils.disposable/Disposable
  (dispose! [_]
    (gdl.utils.disposable/dispose! batch)
    (gdl.utils.disposable/dispose! shape-drawer-texture)
    (run! gdl.utils.disposable/dispose! (vals textures))
    (run! gdl.utils.disposable/dispose! (vals cursors))
    (when default-font
      (gdl.utils.disposable/dispose! default-font)))

  gdl.graphics/Graphics
  (clear-screen! [_ color]
    (screen-utils/clear! color))

  (resize-viewports! [_ width height]
    (viewport/update! ui-viewport    width height :center-camera? true)
    (viewport/update! world-viewport width height :center-camera? false))

  (delta-time [_]
    (graphics/delta-time graphics))

  (frames-per-second [_]
    (graphics/frames-per-second graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key)))

  ; TODO probably not needed I only work with texture-regions
  (texture [_ path]
    (assert (contains? textures path)
            (str "Cannot find texture with path: " (pr-str path)))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (reset! unit-scale world-unit-scale)
                                      (f)
                                      (reset! unit-scale 1))))))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (color/float-bits (color-setter color x y)))))
      (.setView renderer camera)
      ; there is also:
      ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
      ; but right order / visible only ?
      (->> tiled-map
           tiled/layers
           (filter tiled/visible?)
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render renderer))))

  ; FIXME this can be memoized
  ; also good for tiled-map tiles they have to be memoized too
  (image->texture-region [graphics {:keys [image/file
                                           image/bounds]}]
    (assert file)
    (let [texture (gdl.graphics/texture graphics file)]
      (if bounds
        (apply texture/region texture bounds)
        (texture/region texture)))))

(defn do!
  [{:keys [ctx/files
           ctx/graphics]}
   {:keys [textures
           colors ; optional
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]

  (colors/put! colors)

  (let [batch (g2d/sprite-batch)

        shape-drawer-texture (graphics/white-pixel-texture)

        world-unit-scale (float (/ tile-size))

        ui-viewport (graphics/fit-viewport (:width  ui-viewport)
                                           (:height ui-viewport)
                                           (graphics/orthographic-camera))

        {:keys [folder extensions]} textures
        textures-to-load (assets/search (files/internal files folder) extensions)
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [file textures-to-load]
                            [file (graphics/load-texture file)]))

        cursors (create-cursors graphics files cursors cursor-path-format)]
    (map->Graphics {:graphics graphics
                    :textures textures
                    :cursors cursors
                    :default-font (when default-font
                                    (freetype/generate-font (files/internal files (:file default-font))
                                                            (:params default-font)))
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                          world-height (* (:height world-viewport) world-unit-scale)]
                                      (graphics/fit-viewport world-width
                                                             world-height
                                                             (graphics/orthographic-camera :y-down? false
                                                                                           :world-width world-width
                                                                                           :world-height world-height)))
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))

(extend-type com.badlogic.gdx.utils.viewport.FitViewport
  gdl.graphics.viewport/Viewport
  (unproject [this position]
    (graphics/unproject this position)))

(extend-type com.badlogic.gdx.graphics.g2d.TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])
  (region [texture-region x y w h]
    (g2d/texture-region x y w h)))

(extend-type com.badlogic.gdx.graphics.Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (g2d/texture-region texture))
    ([texture x y w h]
     (com.badlogic.gdx.graphics.g2d.TextureRegion. texture
                                                   (int x)
                                                   (int y)
                                                   (int w)
                                                   (int h)))))

(extend-type com.badlogic.gdx.graphics.OrthographicCamera
  gdl.graphics.camera/Camera
  (zoom [this]
    (.zoom this))

  (position [this]
    [(.x (.position this))
     (.y (.position this))])

  (frustum [this]
    (let [frustum-points (take 4 (map vector3/clojurize (.planePoints (.frustum this))))
          left-x   (apply min (map first  frustum-points))
          right-x  (apply max (map first  frustum-points))
          bottom-y (apply min (map second frustum-points))
          top-y    (apply max (map second frustum-points))]
      [left-x right-x bottom-y top-y]))

  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this))

  (viewport-width [this]
    (.viewportWidth this))

  (viewport-height [this]
    (.viewportHeight this))

  (reset-zoom! [cam]
    (gdl.graphics.camera/set-zoom! cam 1))

  (inc-zoom! [cam by]
    (gdl.graphics.camera/set-zoom! cam (max 0.1 (+ (.zoom cam) by)))) )
