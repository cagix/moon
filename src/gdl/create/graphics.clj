(ns gdl.create.graphics
  (:require [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.utils.screen :as screen-utils]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [gdl.files :as files]
            [gdl.graphics]
            [gdl.graphics.g2d.batch :as batch]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.viewport :as viewport]
            [gdl.utils.disposable]
            [gdx.graphics.g2d :as g2d]
            [gdx.graphics.g2d.freetype :as freetype]
            [gdx.graphics.shape-drawer :as sd])
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format
                                      Texture)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn- create-cursor [graphics ^FileHandle file-handle [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. file-handle)
        cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor (color/->obj :white))
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-cursors [graphics files cursors cursor-path-format]
  (update-vals cursors
               (fn [[file hotspot]]
                 (create-cursor graphics
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
    (viewport/update! ui-viewport    width height true)
    (viewport/update! world-viewport width height false))

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
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (batch/set-color! batch (color/->obj :white))
    (batch/set-projection-matrix! batch (.combined (:viewport/camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (batch/end! batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:viewport/camera world-viewport)]
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
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (let [batch (g2d/sprite-batch)

        shape-drawer-texture (white-pixel-texture)

        world-unit-scale (float (/ tile-size))

        ui-viewport (fit-viewport/create (:width  ui-viewport)
                                         (:height ui-viewport)
                                         (orthographic-camera/create))

        textures-to-load (let [[f params] textures]
                           (f files params))
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [[path file-handle] textures-to-load]
                            [path (Texture. file-handle)]))

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
                                      (fit-viewport/create world-width
                                                           world-height
                                                           (orthographic-camera/create :y-down? false
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
