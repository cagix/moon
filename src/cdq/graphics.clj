(ns cdq.graphics
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [cdq.tiled :as tiled]
            [clojure.gdx.utils.screen :as screen-utils]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [gdx.graphics.g2d.freetype :as freetype]
            [gdx.graphics.shape-drawer :as sd])
  (:import (com.badlogic.gdx Files
                             Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport Viewport)
           (cdq.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defprotocol PGraphics
  (clear-screen! [_ color])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path]
           "Returns the already loaded texture whit given file path.")
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (resize-viewports! [_ width height])
  (ui-viewport-height [_])
  (image->texture-region [_ image]
                         "image is `:image/file` (string) & `:image/bounds` `[x y w h]` (optional).

                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds."))

(defmulti ^:private draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   ^TextureRegion texture-region]
  (let [dimensions [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defn- batch-draw! [^SpriteBatch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defmethod draw! :draw/texture-region [[_ ^TextureRegion texture-region [x y]]
                                       {:keys [batch]}]
  (batch-draw! batch
               texture-region
               [x y]
               [(.getRegionWidth  texture-region)
                (.getRegionHeight texture-region)]
               0))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (image->texture-region graphics image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (batch-draw! batch
                 texture-region
                 [(- (float x) (/ (float w) 2))
                  (- (float y) (/ (float h) 2))]
                 [w h]
                 rotation)))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (bitmap-font/draw! (or font default-font)
                     batch
                     {:scale (* (float @unit-scale)
                                (float (or scale 1)))
                      :text text
                      :x x
                      :y y
                      :up? up?
                      :h-align h-align
                      :target-width 0
                      :wrap? false}))

(defmethod draw! :draw/ellipse [[_ position radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd/ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/filled-ellipse [[_ position radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/filled-ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/circle [[_ position radius color]
                               {:keys [shape-drawer]}]
  (sd/circle! shape-drawer position radius color))

(defmethod draw! :draw/filled-circle [[_ position radius color]
                                      {:keys [shape-drawer]}]
  (sd/filled-circle! shape-drawer position radius color))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/filled-rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/arc [[_ center-position radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/arc! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/sector [[_ center-position radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/sector! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/line [[_ start end color]
                             {:keys [shape-drawer]}]
  (sd/line! shape-drawer start end color))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color]
                             {:keys [shape-drawer]}]
  (sd/grid! shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (handle-draws! this draws))))

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

  PGraphics
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
    (let [texture (texture graphics file)
          [x y w h] bounds]
      (if bounds
        (TextureRegion. texture
                        (int x)
                        (int y)
                        (int w)
                        (int h))
        (TextureRegion. texture)))))

(defn create
  [graphics files
   {:keys [colors
           textures
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->obj color-params)))
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
      :textures (into {} (for [[path file-handle] textures]
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
