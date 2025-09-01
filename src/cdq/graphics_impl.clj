(ns cdq.graphics-impl
  (:require [cdq.ctx.graphics]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.orthographic-camera :as orthographic-camera]
            [cdq.gdx.graphics.shape-drawer :as sd]
            [cdq.gdx.tiled :as tiled]
            [clojure.string :as str])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Files
                             Graphics)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align
                                   Disposable
                                   ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport
                                            Viewport)
           (cdq.gdx.graphics OrthogonalTiledMapRenderer
                             ColorSetter)))

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

(def ^:private align-k->value
  {:bottom       Align/bottom
   :bottom-left  Align/bottomLeft
   :bottom-right Align/bottomRight
   :center       Align/center
   :left         Align/left
   :right        Align/right
   :top          Align/top
   :top-left     Align/topLeft
   :top-right    Align/topRight})

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

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- bitmap-font-draw!
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [^BitmapFont font
   batch
   {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align-k->value h-align))]}
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align-k->value (or h-align :center))
           wrap?)
    (.setScale (.getData font) (float old-scale))))

(defn- fit-viewport [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :viewport/width  (Viewport/.getWorldWidth  this)
        :viewport/height (Viewport/.getWorldHeight this)
        :viewport/camera (Viewport/.getCamera      this)))))

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn- degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn- clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

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

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn- unproject-clamp [^Viewport viewport [x y]]
  (let [x (clamp x
                 (.getLeftGutterWidth viewport)
                 (.getRightGutterX    viewport))
        y (clamp y
                 (.getTopGutterHeight viewport)
                 (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))


(defmulti ^:private draw!
  (fn [[k] _graphics]
    k))

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
  (let [texture-region (cdq.ctx.graphics/image->texture-region graphics image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (cdq.ctx.graphics/image->texture-region graphics image)
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
  (bitmap-font-draw! (or font default-font)
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
  (sd/ellipse! shape-drawer position radius-x radius-y (color/->obj color)))

(defmethod draw! :draw/filled-ellipse [[_ position radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/filled-ellipse! shape-drawer position radius-x radius-y (color/->obj color)))

(defmethod draw! :draw/circle [[_ position radius color]
                               {:keys [shape-drawer]}]
  (sd/circle! shape-drawer position radius (color/->obj color)))

(defmethod draw! :draw/filled-circle [[_ position radius color]
                                      {:keys [shape-drawer]}]
  (sd/filled-circle! shape-drawer position radius (color/->obj color)))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/rectangle! shape-drawer x y w h (color/->obj color)))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/filled-rectangle! shape-drawer x y w h (color/->obj color)))

(defmethod draw! :draw/arc [[_ center-position radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/arc! shape-drawer
           center-position
           radius
           (degree->radians start-angle)
           (degree->radians degree)
           (color/->obj color)))

(defmethod draw! :draw/sector [[_ center-position radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/sector! shape-drawer
              center-position
              radius
              (degree->radians start-angle)
              (degree->radians degree)
              (color/->obj color)))

(defmethod draw! :draw/line [[_ start end color]
                             {:keys [shape-drawer]}]
  (sd/line! shape-drawer start end (color/->obj color)))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color]
                             {:keys [shape-drawer]}]
  (sd/grid! shape-drawer leftx bottomy gridw gridh cellw cellh (color/->obj color)))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (cdq.ctx.graphics/handle-draws! this draws))))

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
  cdq.ctx.graphics/Graphics
  (zoom-in! [_ amount]
    (camera/inc-zoom! (:viewport/camera world-viewport) amount))

  (zoom-out! [this amount]
    (cdq.ctx.graphics/zoom-in! this (- amount)))

  (unproject-world [_ position]
    (unproject-clamp world-viewport position))

  (unproject-ui [_ position]
    (unproject-clamp ui-viewport position))

  (handle-draws! [graphics draws]
    (doseq [component draws
            :when component]
      (draw! component graphics)))

  (dispose! [_]
    (Disposable/.dispose batch)
    (Disposable/.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals textures))
    (run! Disposable/.dispose (vals cursors))
    (when default-font
      (Disposable/.dispose default-font)))

  (clear-screen! [_ color]
    (ScreenUtils/clear (color/->obj color)))

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
    (let [^Texture texture (cdq.ctx.graphics/texture graphics file)
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
                                  (orthographic-camera/create))]
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
