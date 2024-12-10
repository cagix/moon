(ns anvil.graphics
  (:require [anvil.tiled :as tiled]
            [anvil.utils :refer [gdx-static-field clamp safe-get degree->radians dispose mapvals]]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Colors Texture Texture$TextureFilter Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)
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

(defn- generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. (.internal Gdx/files file))
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn texture-region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture (int x) (int y) (int w) (int h))))

(defn create [{:keys [default-font cursors viewport world-viewport]}]
  (def batch (SpriteBatch.))
  (def sd-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                 (.setColor Color/WHITE)
                                 (.drawPixel 0 0))
                        texture (Texture. pixmap)]
                    (dispose pixmap)
                    texture))
  (def sd (ShapeDrawer. batch (texture-region sd-texture 1 0 1 1)))
  (def default-font (generate-font default-font))
  (def cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                          (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                            (dispose pixmap)
                            cursor))
                        cursors))
  (def viewport-width  (:width  viewport))
  (def viewport-height (:height viewport))
  (def viewport (FitViewport. viewport-width viewport-height (OrthographicCamera.)))
  (def world-unit-scale (float (/ (:tile-size world-viewport))))
  (def world-viewport-width  (:width  world-viewport))
  (def world-viewport-height (:height world-viewport))
  (def camera (OrthographicCamera.))
  (def world-viewport (let [world-width  (* world-viewport-width  world-unit-scale)
                            world-height (* world-viewport-height world-unit-scale)
                            y-down? false]
                        (.setToOrtho camera y-down? world-width world-height)
                        (FitViewport. world-width world-height camera)))
  (def tiled-map-renderer
    (memoize (fn [tiled-map]
               (OrthogonalTiledMapRenderer. tiled-map (float world-unit-scale) batch)))))

(defn cleanup []
  (dispose batch)
  (dispose sd-texture)
  (dispose default-font)
  (run! dispose (vals cursors)))

(defn resize [w h]
  (Viewport/.update viewport w h true)
  (Viewport/.update world-viewport w h false))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn ->color
  ([r g b]
   (->color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a)))
  (^Color [c]
          (cond (= Color (class c)) c
                (keyword? c) (gdx-static-field "graphics.Color" c)
                (vector? c) (apply ->color c)
                :else (throw (ex-info "Cannot understand color" c)))))

(defn add-color [name-str color]
  (Colors/put name-str (->color color)))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn ->texture-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region (int x) (int y) (int w) (int h)))

(defn texture-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- sd-color [color]
  (.setColor sd (->color color)))

(defn ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.ellipse sd
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.filledEllipse sd
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle [[x y] radius color]
  (sd-color color)
  (.circle sd
           (float x)
           (float y)
           (float radius)))

(defn filled-circle [[x y] radius color]
  (sd-color color)
  (.filledCircle sd
                 (float x)
                 (float y)
                 (float radius)))

(defn arc [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (.arc sd
        (float center-x)
        (float center-y)
        (float radius)
        (float (degree->radians start-angle))
        (float (degree->radians degree))))

(defn sector [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (.sector sd
           (float center-x)
           (float center-y)
           (float radius)
           (float (degree->radians start-angle))
           (float (degree->radians degree))))

(defn rectangle [x y w h color]
  (sd-color color)
  (.rectangle sd
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle [x y w h color]
  (sd-color color)
  (.filledRectangle sd
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line [[sx sy] [ex ey] color]
  (sd-color color)
  (.line sd
         (float sx)
         (float sy)
         (float ex)
         (float ey)))

(defn grid [leftx bottomy gridw gridh cellw cellh color]
  (sd-color color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line [leftx liney] [rightx liney]))))

(defn with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth sd)]
    (.setDefaultLineWidth sd (* width old-line-width))
    (draw-fn)
    (.setDefaultLineWidth sd old-line-width)))

(defn set-cursor [cursor-key]
  (.setCursor Gdx/graphics (safe-get cursors cursor-key)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
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
  (if color (.setColor batch white)))

(def ^:dynamic ^:private *unit-scale* 1)

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

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
           (+ (float y)
              (float (if up?
                       (text-height font text)
                       0)))
           (float 0) ; target-width
           (gdx-align (or h-align
                          :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn draw-image
  [{:keys [texture-region color] :as image} position]
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

(defn- draw-on-viewport [batch viewport draw-fn]
  (.setColor batch white) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (Viewport/.getCamera viewport)))
  (.begin batch)
  (draw-fn)
  (.end batch))

(defn draw-with [viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width unit-scale
                       (fn []
                         (binding [*unit-scale* unit-scale]
                           (draw-fn))))))

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

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position viewport)))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport world-unit-scale render-fn))

(defn- draw-tiled-map* [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
  (.setColorSetter this (reify ColorSetter
                          (apply [_ color x y]
                            (color-setter color x y))))
  (.setView this camera)
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render this)))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (draw-tiled-map* (tiled-map-renderer tiled-map)
                   tiled-map
                   color-setter
                   camera))
