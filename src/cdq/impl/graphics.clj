(ns cdq.impl.graphics
  (:require [cdq.ctx :as ctx]
            [cdq.camera :as camera]
            [cdq.graphics :as graphics]
            [cdq.interop :as interop]
            [cdq.tiled :as tiled]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Texture OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont TextureRegion)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- set-color! [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (interop/->color color)))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw-text! [{:keys [^BitmapFont font
                           scale
                           batch
                           x
                           y
                           text
                           h-align
                           up?]}]
  (let [data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale (float scale)))
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

(defn- fit-viewport
  ([width height]
   (fit-viewport width height (OrthographicCamera.)))
  ([width height camera]
   (proxy [FitViewport clojure.lang.ILookup] [width height camera]
     (valAt
       ([key]
        (interop/k->viewport-field this key))
       ([key _not-found]
        (interop/k->viewport-field this key))))))

(defn- ->world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

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

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defrecord Graphics [world-viewport
                     get-tiled-map-renderer
                     unit-scale
                     ui-viewport]
  graphics/Graphics
  (mouse-position [_]
    ; TODO mapv int needed?
    (mapv int (unproject-mouse-position ui-viewport)))

  (world-mouse-position [_]
    ; TODO clamping only works for gui-viewport ? check. comment if true
    ; TODO ? "Can be negative coordinates, undefined cells."
    (unproject-mouse-position world-viewport))

  (pixels->world-units [_ pixels]
    (* (int pixels) ctx/world-unit-scale))

  (draw-image [_ {:keys [texture-region color] :as image} position]
    (draw-texture-region ctx/batch
                         texture-region
                         position
                         (unit-dimensions image @unit-scale)
                         0 ; rotation
                         color))

  (draw-rotated-centered [_ {:keys [texture-region color] :as image} rotation [x y]]
    (let [[w h] (unit-dimensions image @unit-scale)]
      (draw-texture-region ctx/batch
                           texture-region
                           [(- (float x) (/ (float w) 2))
                            (- (float y) (/ (float h) 2))]
                           [w h]
                           rotation
                           color)))

  (draw-text [_ {:keys [font scale x y text h-align up?]}]
    (draw-text! {:font (or font ctx/default-font)
                 :scale (* (float @unit-scale)
                           (float (or scale 1)))
                 :batch ctx/batch
                 :x x
                 :y y
                 :text text
                 :h-align h-align
                 :up? up?}))

  (draw-ellipse [_ [x y] radius-x radius-y color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.ellipse ctx/shape-drawer
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (draw-filled-ellipse [_ [x y] radius-x radius-y color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.filledEllipse ctx/shape-drawer
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (draw-circle [_ [x y] radius color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.circle ctx/shape-drawer
             (float x)
             (float y)
             (float radius)))

  (draw-filled-circle [_ [x y] radius color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.filledCircle ctx/shape-drawer
                   (float x)
                   (float y)
                   (float radius)))

  (draw-arc [_ [center-x center-y] radius start-angle degree color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.arc ctx/shape-drawer
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (draw-sector [_ [center-x center-y] radius start-angle degree color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.sector ctx/shape-drawer
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (draw-rectangle [_ x y w h color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.rectangle ctx/shape-drawer
                (float x)
                (float y)
                (float w)
                (float h)))

  (draw-filled-rectangle [_ x y w h color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.filledRectangle ctx/shape-drawer
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (draw-line [_ [sx sy] [ex ey] color]
    (set-color! ctx/shape-drawer color)
    (ShapeDrawer/.line ctx/shape-drawer
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [_ width draw-fn]
    (let [old-line-width (ShapeDrawer/.getDefaultLineWidth ctx/shape-drawer)]
      (ShapeDrawer/.setDefaultLineWidth ctx/shape-drawer (float (* width old-line-width)))
      (draw-fn)
      (ShapeDrawer/.setDefaultLineWidth ctx/shape-drawer (float old-line-width))))

  (draw-grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* (float gridw) (float cellw))
          h (* (float gridh) (float cellh))
          topy (+ (float bottomy) (float h))
          rightx (+ (float leftx) (float w))]
      (doseq [idx (range (inc (float gridw)))
              :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
        (graphics/draw-line this [linex topy] [linex bottomy] color))
      (doseq [idx (range (inc (float gridh)))
              :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
        (graphics/draw-line this [leftx liney] [rightx liney] color))))

  (sub-sprite [_ sprite [x y w h]]
    (sprite* (TextureRegion. ^TextureRegion (:texture-region sprite)
                             (int x)
                             (int y)
                             (int w)
                             (int h))
             ctx/world-unit-scale))

  (sprite-sheet [_ texture tilew tileh]
    {:image (sprite* (TextureRegion. ^Texture texture)
                     ctx/world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (from-sheet [this {:keys [image tilew tileh]} [x y]]
    (graphics/sub-sprite this
                         image
                         [(* x tilew)
                          (* y tileh)
                          tilew
                          tileh]))

  (sprite [_ texture]
    (sprite* (TextureRegion. ^Texture texture)
             ctx/world-unit-scale)))

(defn create [{:keys [tile-size
                      world-viewport
                      ui-viewport]}]
  (map->Graphics
   {:world-viewport (->world-viewport ctx/world-unit-scale world-viewport)
    :get-tiled-map-renderer (memoize (fn [tiled-map]
                                       (tiled/renderer tiled-map
                                                       ctx/world-unit-scale
                                                       ctx/batch)))
    :ui-viewport (fit-viewport (:width  ui-viewport)
                               (:height ui-viewport))
    :unit-scale (atom 1)}))
