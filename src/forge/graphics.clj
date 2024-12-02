(ns forge.graphics
  (:require [forge.tiled :as tiled])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Colors Texture Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d BitmapFont Batch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils.viewport Viewport)
           (com.badlogic.gdx.math Vector2)
           (space.earlygrey.shapedrawer ShapeDrawer)
           (forge OrthogonalTiledMapRenderer ColorSetter)))

(def world-viewport-width 1440)
(def world-viewport-height 900)
(def gui-viewport-width 1440)
(def gui-viewport-height 900)

(defn- draw-texture-region [^Batch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w ; width height
         h
         1 ; scaling factor
         1
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- texture-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} world-unit-scale scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [world-unit-scale texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions world-unit-scale 1) ; = scale 1
      map->Sprite))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn truetype-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str-split #"\n")
      count
      (* (.getLineHeight font))))

(defn add-color [name-str color]
  (Colors/put name-str (->gdx-color color)))

(declare ^Batch batch
         ^ShapeDrawer shape-drawer
         ^BitmapFont default-font
         cached-map-renderer
         world-unit-scale
         ^Viewport world-viewport
         ^Viewport gui-viewport
         cursors)

(def ^:dynamic ^:private *unit-scale* 1)

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [^Viewport viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (.getLeftGutterWidth viewport)
                       (.getRightGutterX viewport))
        mouse-y (clamp (.getY Gdx/input)
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

(defn ->texture-region
  ([path]
   (TextureRegion. ^Texture (get asset-manager path)))
  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defn image [path]
  (sprite* world-unit-scale (->texture-region path)))

(defn sub-image [image bounds]
  (sprite* world-unit-scale
           (->texture-region (:texture-region image) bounds)))

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
           (gdx-align (or h-align :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn draw-image [{:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- sd-color [color]
  (.setColor shape-drawer (->gdx-color color)))

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
  (.arc shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.sector shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

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

(defn set-cursor [cursor-key]
  (.setCursor Gdx/graphics (safe-get cursors cursor-key)))

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite (sprite-sheet file tilew tileh)
              [(int (/ sprite-x tilew))
               (int (/ sprite-y tileh))]))
    (image file)))

(defmethod edn->value :s/image [_ edn]
  (edn->image edn))
