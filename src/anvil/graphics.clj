(ns anvil.graphics
  (:require [anvil.graphics.color :as color :refer [->color]]
            [anvil.graphics.shape-drawer :as sd]
            [clojure.string :as str]
            [anvil.utils :refer [clamp safe-get degree->radians]])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d BitmapFont TextureRegion)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn ->texture-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region (int x) (int y) (int w) (int h)))

(defn texture-region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture (int x) (int y) (int w) (int h))))

(defn texture-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(declare batch
         sd
         default-font
         cursors)

(defn- sd-color [color]
  (sd/set-color sd (->color color)))

(defn ellipse [position radius-x radius-y color]
  (sd-color color)
  (sd/ellipse sd position radius-x radius-y))

(defn filled-ellipse [position radius-x radius-y color]
  (sd-color color)
  (sd/filled-ellipse sd position radius-x radius-y))

(defn circle [position radius color]
  (sd-color color)
  (sd/circle sd position radius))

(defn filled-circle [position radius color]
  (sd-color color)
  (sd/filled-circle sd position radius))

(defn arc [center radius start-angle degree color]
  (sd-color color)
  (sd/arc sd
          center
          radius
          (degree->radians start-angle)
          (degree->radians degree)))

(defn sector [center radius start-angle degree color]
  (sd-color color)
  (sd/sector sd
             center
             radius
             (degree->radians start-angle)
             (degree->radians degree)))

(defn rectangle [x y w h color]
  (sd-color color)
  (sd/rectangle sd x y w h))

(defn filled-rectangle [x y w h color]
  (sd-color color)
  (sd/filled-rectangle sd x y w h))

(defn line [start end color]
  (sd-color color)
  (sd/line sd start end))

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
  (let [old-line-width (sd/default-line-width sd)]
    (sd/set-default-line-width sd (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width sd old-line-width)))

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
  (if color (.setColor batch color/white)))

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
  (.setColor batch color/white) ; fix scene2d.ui.tooltip flickering
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
(defn unproject-mouse-position
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
