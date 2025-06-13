(ns gdl.create.graphics
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.shape-drawer :as sd]
            [clojure.gdx.tiled :as tiled]
            [gdl.files :as files]
            [gdl.graphics]
            [gdl.graphics.camera]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics.viewport]
            [gdl.utils.disposable]
            [gdx.graphics :as graphics]
            [gdx.graphics.color :as color]
            [gdx.graphics.colors :as colors]
            [gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [gdx.graphics.g2d.freetype :as freetype])
  (:import (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defmulti ^:private draw!
  (fn [[k] _graphics]
    k))

(defrecord Graphics [graphics
                     textures
                     cursors
                     default-font
                     world-unit-scale
                     ui-viewport
                     world-viewport
                     batch
                     unit-scale
                     shape-drawer-texture
                     shape-drawer
                     tiled-map-renderer]
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
    (gdx/clear-screen! color))

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
    (gdx/draw-on-viewport! batch
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
        (texture/region texture))))

  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this))))

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
  (let [batch (gdx/sprite-batch)
        shape-drawer-texture (gdx/white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (gdx/fit-viewport (:width  ui-viewport)
                                      (:height ui-viewport)
                                      (gdx/orthographic-camera))
        {:keys [folder extensions]} textures
        textures-to-load (gdx/find-assets (files/internal files folder) extensions)
        ;(println "load-textures (count textures): " (count textures))
        textures (into {} (for [file textures-to-load]
                            [file (gdx/load-texture file)]))
        cursors (update-vals cursors
                             (fn [[file [hotspot-x hotspot-y]]]
                               (let [pixmap (graphics/pixmap (files/internal files (format cursor-path-format file)))
                                     cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                                 (.dispose pixmap)
                                 cursor)))]
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
                                      (gdx/fit-viewport world-width
                                                        world-height
                                                        (gdx/orthographic-camera :y-down? false
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
    (gdx/unproject this position)))

(extend-type com.badlogic.gdx.graphics.g2d.TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])
  (region [texture-region x y w h]
    (gdx/texture-region x y w h)))

(extend-type com.badlogic.gdx.graphics.Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (gdx/texture-region texture))
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
    (let [frustum-points (take 4 (map gdx/vector3->clj-vec (.planePoints (.frustum this))))
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

(defmethod draw! :draw/texture-region [[_ texture-region [x y]]
                                       {:keys [batch]}]
  (gdx/draw-texture-region! batch
                            texture-region
                            [x y]
                            (texture-region/dimensions texture-region)
                            0  ;rotation
                            ))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   texture-region]
  (let [dimensions (texture-region/dimensions texture-region)]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)]
    (gdx/draw-texture-region! batch
                              texture-region
                              position
                              (texture-region-drawing-dimensions graphics texture-region)
                              0 ; rotation
                              )))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (gdl.graphics/image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (gdx/draw-texture-region! batch
                              texture-region
                              [(- (float x) (/ (float w) 2))
                               (- (float y) (/ (float h) 2))]
                              [w h]
                              rotation
                              )))

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
      (gdl.graphics/handle-draws! this draws))))
