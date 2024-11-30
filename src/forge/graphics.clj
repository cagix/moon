(ns forge.graphics
  (:require [clojure.string :as str]
            [forge.assets :as assets]
            [forge.graphics.image :as image]
            [forge.gdx :as gdx]
            [forge.tiled :as tiled]
            [forge.math.utils :as math])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Colors OrthographicCamera Texture Texture$TextureFilter Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d BitmapFont Batch SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils Disposable ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.badlogic.gdx.math Vector2)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- truetype-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn add-color [name-str color]
  (Colors/put name-str (gdx/->color color)))

(defn clear-screen []
  (ScreenUtils/clear black))

(def tile-size 48)

(def world-viewport-width 1440)
(def world-viewport-height 900)

(def gui-viewport-width 1440)
(def gui-viewport-height 900)

(declare ^Batch batch
         ^:private ^ShapeDrawer shape-drawer
         ^:private ^Texture shape-drawer-texture
         ^:private ^BitmapFont default-font
         ^:private cached-map-renderer
         ^:private world-unit-scale
         ^Viewport world-viewport
         ^Viewport gui-viewport
         cursors)

(def ^:dynamic ^:private *unit-scale* 1)

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [^Viewport viewport]
  (let [mouse-x (math/clamp (.getX Gdx/input)
                            (.getLeftGutterWidth viewport)
                            (.getRightGutterX viewport))
        mouse-y (math/clamp (.getY Gdx/input)
                            (.getTopGutterHeight viewport)
                            (.getTopGutterY viewport))
        coords (.unproject viewport (Vector2. mouse-x mouse-y))]
    [(.x coords) (.y coords)]))

(defn gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position gui-viewport)))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn world-camera []
  (.getCamera world-viewport))

(defn texture-region
  ([path]
   (TextureRegion. (assets/texture path)))
  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defn image [path]
  (image/create world-unit-scale (texture-region path)))

(defn sub-image [image bounds]
  (image/create world-unit-scale
                (texture-region (:texture-region image) bounds)))

(defn sprite-sheet [path tilew tileh]
  {:image (image path)
   :tilew tilew
   :tileh tileh})

(defn sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font x y text h-align up? scale]}]
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))]
    (.setScale data (* old-scale
                       (float *unit-scale*)
                       (float (or scale 1))))
    (.draw font
           batch
           (str text)
           (float x)
           (+ (float y) (float (if up? (text-height font text) 0)))
           (float 0) ; target-width
           (gdx/align (or h-align :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn draw-image [image position]
  (image/draw batch *unit-scale* image position))

(defn draw-centered [image position]
  (image/draw-centered batch *unit-scale* image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch *unit-scale* image rotation position))

(defn- sd-color [color]
  (.setColor shape-drawer (gdx/->color color)))

(defn draw-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn draw-circle [[x y] radius color]
  (sd-color color)
  (.circle shape-drawer (float x) (float y) (float radius)))

(defn draw-filled-circle [[x y] radius color]
  (sd-color color)
  (.filledCircle shape-drawer (float x) (float y) (float radius)))

(defn draw-arc [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.arc shape-drawer (float centre-x) (float centre-y) (float radius) (math/degree->radians start-angle) (math/degree->radians degree)))

(defn draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.sector shape-drawer (float centre-x) (float centre-y) (float radius) (math/degree->radians start-angle) (math/degree->radians degree)))

(defn draw-rectangle [x y w h color]
  (sd-color color)
  (.rectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn draw-filled-rectangle [x y w h color]
  (sd-color color)
  (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn draw-line [[sx sy] [ex ey] color]
  (sd-color color)
  (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd-color color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw-line shape-drawer [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw-line shape-drawer [leftx liney] [rightx liney]))))

(defn with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth shape-drawer)]
    (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth shape-drawer (float old-line-width))))

(defn- draw-with [^Viewport viewport unit-scale draw-fn]
  (.setColor batch white) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (.getCamera viewport)))
  (.begin batch)
  (with-line-width unit-scale
    #(binding [*unit-scale* unit-scale]
       (draw-fn)))
  (.end batch))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport world-unit-scale render-fn))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (let [^OrthogonalTiledMapRenderer map-renderer (cached-map-renderer tiled-map)]
    (.setColorSetter map-renderer (reify ColorSetter
                                    (apply [_ color x y]
                                      (color-setter color x y))))
    (.setView map-renderer (world-camera))
    (->> tiled-map
         tiled/layers
         (filter tiled/visible?)
         (map (partial tiled/layer-index tiled-map))
         int-array
         (.render map-renderer))))

(defn- make-cursors [cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                   cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
               (.dispose pixmap)
               cursor))
           cursors))

(defn set-cursor [cursor-key]
  (.setCursor Gdx/graphics (safe-get cursors cursor-key)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn init [{:keys [cursors]}]
  (bind-root #'batch (SpriteBatch.))
  (bind-root #'shape-drawer-texture (white-pixel-texture))
  (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
  (bind-root #'default-font (truetype-font
                             {:file (.internal Gdx/files "fonts/exocet/films.EXL_____.ttf")
                              :size 16
                              :quality-scaling 2}))
  (bind-root #'world-unit-scale (float (/ tile-size)))
  (bind-root #'world-viewport (let [world-width  (* world-viewport-width world-unit-scale)
                                    world-height (* world-viewport-height world-unit-scale)
                                    camera (OrthographicCamera.)
                                    y-down? false]
                                (.setToOrtho camera y-down? world-width world-height)
                                (FitViewport. world-width world-height camera)))
  (bind-root #'cached-map-renderer (memoize
                                    (fn [tiled-map]
                                      (OrthogonalTiledMapRenderer. tiled-map (float world-unit-scale) batch))))
  (bind-root #'gui-viewport (FitViewport. gui-viewport-width
                                          gui-viewport-height
                                          (OrthographicCamera.)))
  (bind-root #'cursors (make-cursors cursors)))

(defn dispose []
  (.dispose batch)
  (.dispose shape-drawer-texture)
  (.dispose default-font)
  (run! Disposable/.dispose (vals cursors)))

(defn resize [w h]
  (.update gui-viewport   w h true)
  (.update world-viewport w h))
