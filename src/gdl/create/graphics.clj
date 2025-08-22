(ns gdl.create.graphics
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.utils.screen :as screen-utils]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [gdl.graphics]
            [gdx.graphics.g2d.freetype :as freetype]
            [gdx.graphics.shape-drawer :as sd])
  (:import (com.badlogic.gdx Files
                             Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport Viewport)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defrecord RGraphics
  [^SpriteBatch batch
   cursors
   default-font
   ^Graphics graphics
   shape-drawer-texture
   shape-drawer
   textures
   tiled-map-renderer
   ^Viewport ui-viewport
   unit-scale
   world-unit-scale
   ^Viewport world-viewport]
  Disposable
  (dispose [_]
    (Disposable/.dispose batch)
    (Disposable/.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals textures))
    (run! Disposable/.dispose (vals cursors))
    (when default-font
      (Disposable/.dispose default-font)))

  gdl.graphics/Graphics
  (clear-screen! [_ color]
    (screen-utils/clear! color))

  (resize-viewports! [_ width height]
    (.update ui-viewport    width height true)
    (.update world-viewport width height false))

  (delta-time [_]
    (.getDeltaTime graphics))

  (frames-per-second [_]
    (.getFramesPerSecond graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (.setCursor graphics (get cursors cursor-key)))

  ; TODO probably not needed I only work with texture-regions
  (texture [_ path]
    (assert (contains? textures path)
            (str "Cannot find texture with path: " (pr-str path)))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (.setColor batch (color/->obj :white))
    (.setProjectionMatrix batch (.combined (:viewport/camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

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
    (let [texture (gdl.graphics/texture graphics file)
          [x y w h] bounds]
      (if bounds
        (TextureRegion. texture
                        (int x)
                        (int y)
                        (int w)
                        (int h))
        (TextureRegion. texture)))))

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
  (let [batch (SpriteBatch.)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor (color/->obj :white))
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (fit-viewport/create (:width  ui-viewport)
                                         (:height ui-viewport)
                                         (orthographic-camera/create))]
    (map->RGraphics
     {:graphics graphics
      :textures (into {} (for [[path file-handle] (let [[f params] textures]
                                                    (f files params))]
                           [path (Texture. file-handle)]))
      :cursors (update-vals cursors
                            (fn [[file [hotspot-x hotspot-y]]]
                              (let [pixmap (Pixmap. (Files/.internal files (format cursor-path-format file)))
                                    cursor (Graphics/.newCursor graphics pixmap hotspot-x hotspot-y)]
                                (.dispose pixmap)
                                cursor)))
      :default-font (when default-font
                      (freetype/generate-font (Files/.internal files (:file default-font))
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
      :shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1))
      :tiled-map-renderer (memoize (fn [tiled-map]
                                     (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                  (float world-unit-scale)
                                                                  batch)))})))
