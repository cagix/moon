(ns cdq.graphics
  (:require [cdq.gdx.interop :as interop]
            [cdq.graphics.camera :as camera]
            [cdq.utils :as utils]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(declare ^:private ^Batch batch
         ^:private ^Texture shape-drawer-texture
         ^:private ^ShapeDrawer shape-drawer
         ^:private cursors
         ^:private ^BitmapFont default-font
         world-unit-scale
         world-viewport)

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn- load-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn fit-viewport [width height camera]
  (proxy [FitViewport clojure.lang.ILookup] [width height camera]
    (valAt
      ([key]
       (interop/k->viewport-field this key))
      ([key _not-found]
       (interop/k->viewport-field this key)))))

(defn- ->world-viewport [world-unit-scale config]
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

(defn create! [{:keys [cursors
                       default-font
                       tile-size
                       world-viewport]}]
  (.bindRoot #'batch (SpriteBatch.))
  (.bindRoot #'shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                                   (.setColor Color/WHITE)
                                                   (.drawPixel 0 0))
                                          texture (Texture. pixmap)]
                                      (.dispose pixmap)
                                      texture))
  (.bindRoot #'shape-drawer (ShapeDrawer. batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
  (.bindRoot #'cursors (utils/mapvals
                        (fn [[file [hotspot-x hotspot-y]]]
                          (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                            (.dispose pixmap)
                            cursor))
                        cursors))
  (.bindRoot #'default-font (load-font default-font))
  (.bindRoot #'world-unit-scale (float (/ tile-size)))
  (.bindRoot #'world-viewport (->world-viewport world-unit-scale world-viewport)))

(defn dispose! []
  (.dispose batch)
  (.dispose shape-drawer-texture)
  (run! Disposable/.dispose (vals cursors))
  (.dispose default-font))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (.getY Gdx/input)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (let [v2 (Viewport/.unproject viewport (Vector2. mouse-x mouse-y))]
      [(.x v2) (.y v2)])))

(defn mouse-position [ui-viewport]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position ui-viewport)))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [^Batch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn draw-image
  [{:keys [cdq.context/unit-scale]}
   {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [cdq.context/unit-scale]}
   {:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [c image position]
  (draw-rotated-centered c image 0 position))

(defn set-camera-position! [position]
  (camera/set-position! (:camera world-viewport) position))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (BitmapFont/.getLineHeight font))))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [cdq.context/unit-scale]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))

(defn- sd-set-color! [color]
  (.setColor shape-drawer (interop/->color color)))

(defn ellipse [[x y] radius-x radius-y color]
  (sd-set-color! color)
  (.ellipse shape-drawer
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (sd-set-color! color)
  (.filledEllipse shape-drawer
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle [[x y] radius color]
  (sd-set-color! color)
  (.circle shape-drawer
           (float x)
           (float y)
           (float radius)))

(defn filled-circle [[x y] radius color]
  (sd-set-color! color)
  (.filledCircle shape-drawer
                 (float x)
                 (float y)
                 (float radius)))

(defn arc [[center-x center-y] radius start-angle degree color]
  (sd-set-color! color)
  (.arc shape-drawer
        (float center-x)
        (float center-y)
        (float radius)
        (float (degree->radians start-angle))
        (float (degree->radians degree))))

(defn sector [[center-x center-y] radius start-angle degree color]
  (sd-set-color! color)
  (.sector shape-drawer
           (float center-x)
           (float center-y)
           (float radius)
           (float (degree->radians start-angle))
           (float (degree->radians degree))))

(defn rectangle [x y w h color]
  (sd-set-color! color)
  (.rectangle shape-drawer
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle [x y w h color]
  (sd-set-color! color)
  (.filledRectangle shape-drawer
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line [[sx sy] [ex ey] color]
  (sd-set-color! color)
  (.line shape-drawer
         (float sx)
         (float sy)
         (float ex)
         (float ey)))

(defn with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth shape-drawer)]
    (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth shape-drawer (float old-line-width))))

(defn grid [leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line shape-drawer [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line shape-drawer [leftx liney] [rightx liney] color))))

(defn draw-on-world-view! [context draw-fns]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
  (.begin batch)
  (with-line-width world-unit-scale
    (fn []
      (let [context (assoc context :cdq.context/unit-scale world-unit-scale)]
        (doseq [f draw-fns]
          (f context)))))
  (.end batch))

(defn set-cursor! [cursor-key]
  (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))
