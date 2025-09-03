(ns cdq.graphics-impl
  (:require [cdq.graphics]
            [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.shape-drawer :as sd]
            [cdq.gdx.tiled :as tiled]
            [clojure.string :as str])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Files
                             Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Colors
                                      OrthographicCamera
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector3)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)
           (cdq.gdx.graphics OrthogonalTiledMapRenderer
                             ColorSetter)))

(defn- vector3-clojurize [^Vector3 v3]
  [(.x v3)
   (.y v3)
   (.z v3)])

(defn- orthographic-camera
  ([]
   (proxy [OrthographicCamera ILookup] []
     (valAt [k]
       (let [^OrthographicCamera this this]
         (case k
           :camera/combined (.combined this)
           :camera/zoom (.zoom this)
           :camera/frustum {:frustum/plane-points (mapv vector3-clojurize (.planePoints (.frustum this)))}
           :camera/position (vector3-clojurize (.position this))
           :camera/viewport-width  (.viewportWidth  this)
           :camera/viewport-height (.viewportHeight this))))))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (orthographic-camera)
     (OrthographicCamera/.setToOrtho y-down? world-width world-height))))

(comment
 Nearest ; Fetch the nearest texel that best maps to the pixel on screen.
Linear ; Fetch four nearest texels that best maps to the pixel on screen.
MipMap ; @see TextureFilter#MipMapLinearLinear
MipMapNearestNearest ; Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a nearest filter.
MipMapLinearNearest ; Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a linear filter.
MipMapNearestLinear ; Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images, combining them to the final output pixel.
MipMapLinearLinear ; Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two images, combining them to the final output pixel.
 )

(let [mapping {:linear Texture$TextureFilter/Linear}]
  (defn texture-filter-k->value [k]
    (when-not (contains? mapping k)
      (throw (IllegalArgumentException. (str "Unknown Key: " k ". \nOptions are:\n" (sort (keys mapping))))))
    (k mapping)))

(defn- bitmap-font-configure! [^BitmapFont font {:keys [scale enable-markup? use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)

(defn- create-font-params [{:keys [size
                                   min-filter
                                   mag-filter]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    (set! (.minFilter params) min-filter)
    (set! (.magFilter params) mag-filter)
    params))

(defn- generate-font [file-handle {:keys [size
                                          quality-scaling
                                          enable-markup?
                                          use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator
                            (create-font-params {:size (* size quality-scaling)
                                                 ; :texture-filter/linear because scaling to world-units
                                                 :min-filter (texture-filter-k->value :linear)
                                                 :mag-filter (texture-filter-k->value :linear)}))]
    (bitmap-font-configure! font {:scale (/ quality-scaling)
                                  :enable-markup? enable-markup?
                                  :use-integer-positions? use-integer-positions?})))

(defn- fit-viewport [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :viewport/width  (Viewport/.getWorldWidth  this)
        :viewport/height (Viewport/.getWorldHeight this)
        :viewport/camera (Viewport/.getCamera      this)))))

(defn- recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- search-files [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (Files/.internal files path)])
       (recursively-search (Files/.internal files folder) extensions)))

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
  cdq.graphics/Graphics
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
    (.setProjectionMatrix batch (:camera/combined (:viewport/camera world-viewport)))
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
    (let [^Texture texture (cdq.graphics/texture graphics file)
          [x y w h] bounds]
      (if bounds
        (TextureRegion. texture
                        (int x)
                        (int y)
                        (int w)
                        (int h))
        (TextureRegion. texture)))))

(defn create!
  [{:keys [graphics files]}
   {:keys [colors
           cursors ; optional
           cursor-path-format ; optional
           default-font ; optional, could use gdx included (BitmapFont.)
           tile-size
           ui-viewport
           world-viewport]}]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->obj color-params)))
  (let [textures (search-files files
                               {:folder "resources/"
                                :extensions #{"png" "bmp"}})
        batch (SpriteBatch.)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor (color/->obj :white))
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (fit-viewport (:width  ui-viewport)
                                  (:height ui-viewport)
                                  (orthographic-camera))]
    (map->RGraphics
     {:graphics graphics
      :textures (into {} (for [[path file-handle] textures]
                           [path (Texture. ^FileHandle file-handle)]))
      :cursors (update-vals cursors
                            (fn [[file [hotspot-x hotspot-y]]]
                              (let [pixmap (Pixmap. (Files/.internal files (format cursor-path-format file)))
                                    cursor (Graphics/.newCursor graphics pixmap hotspot-x hotspot-y)]
                                (.dispose pixmap)
                                cursor)))
      :default-font (when default-font
                      (generate-font (Files/.internal files (:file default-font))
                                     (:params default-font)))
      :world-unit-scale world-unit-scale
      :ui-viewport ui-viewport
      :world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                            world-height (* (:height world-viewport) world-unit-scale)]
                        (fit-viewport world-width
                                      world-height
                                      (orthographic-camera :y-down? false
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
