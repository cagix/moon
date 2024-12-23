(ns gdl.graphics
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as font]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.input :as input]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.math.utils :refer [clamp degree->radians]]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.string :as str]
            [gdl.context :as ctx]
            [gdl.tiled :as tiled])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn setup-tiled-map-renderer [world-unit-scale batch]
  (def tiled-map-renderer
    (memoize (fn [tiled-map]
               (OrthogonalTiledMapRenderer. tiled-map
                                            (float world-unit-scale)
                                            batch)))))

(defn setup-shape-drawer []
  (def sd-texture (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                                 (pixmap/set-color color/white)
                                 (pixmap/draw-pixel 0 0))
                        texture (texture/create pixmap)]
                    (dispose pixmap)
                    texture))
  (def sd (sd/create ctx/batch (texture-region/create sd-texture 1 0 1 1))))

(defn dispose-shape-drawer []
  (dispose sd-texture))

(defn resize-world-viewport [w h]
  (viewport/update ctx/world-viewport w h :center-camera? false))

(def black color/black)
(def white color/white)

(defn- color? [object]
  (= com.badlogic.gdx.graphics.Color (class object)))

(defn ->color
  ([r g b]
   (color/create r g b))
  ([r g b a]
   (color/create r g b a))
  ([c]
   (cond (color? c) c
         (keyword? c) (interop/k->color c)
         (vector? c) (apply ->color c)
         :else (throw (ex-info "Cannot understand color" c)))))

(defn add-color [name-str color]
  (colors/put name-str (->color color)))

(def frames-per-second g/frames-per-second)
(def delta-time        g/delta-time)

(defn- sd-color [color]
  (sd/set-color sd (->color color)))

(defn ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/ellipse sd x y radius-x radius-y))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/filled-ellipse sd x y radius-x radius-y))

(defn circle [[x y] radius color]
  (sd-color color)
  (sd/circle sd x y radius))

(defn filled-circle [[x y] radius color]
  (sd-color color)
  (sd/filled-circle sd x y radius))

(defn arc [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (sd/arc sd center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (sd/sector sd center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [x y w h color]
  (sd-color color)
  (sd/rectangle sd x y w h))

(defn filled-rectangle [x y w h color]
  (sd-color color)
  (sd/filled-rectangle sd x y w h))

(defn line [[sx sy] [ex ey] color]
  (sd-color color)
  (sd/line sd sx sy ex ey))

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

(def ^:dynamic ^:private *unit-scale* 1)

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (font/line-height font))))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font x y text h-align up? scale]}]
  (let [font (or font ctx/default-font)
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float *unit-scale*)
                            (float (or scale 1))))
    (font/draw :font font
               :batch ctx/batch
               :text text
               :x x
               :y (+ y (if up? (text-height font text) 0))
               :align (interop/k->align (or h-align :center)))
    (font/set-scale data old-scale)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
  (if color (batch/set-color batch color))
  (batch/draw batch
              texture-region
              :x x
              :y y
              :origin-x (/ (float w) 2) ; rotation origin
              :origin-y (/ (float h) 2)
              :width w
              :height h
              :scale-x 1
              :scale-y 1
              :rotation rotation)
  (if color (batch/set-color batch white)))

(defn draw-image
  [{:keys [texture-region color] :as image} position]
  (draw-texture-region ctx/batch
                       texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region ctx/batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- draw-on-viewport [batch viewport draw-fn]
  (batch/set-color batch white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (draw-fn)
  (batch/end batch))

(defn draw-with [viewport unit-scale draw-fn]
  (draw-on-viewport ctx/batch
                    viewport
                    #(with-line-width unit-scale
                       (fn []
                         (binding [*unit-scale* unit-scale]
                           (draw-fn))))))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (input/x)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input/y)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (viewport/unproject viewport mouse-x mouse-y)))

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position ctx/viewport)))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position ctx/world-viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) ctx/world-unit-scale))

(defn draw-on-world-view [render-fn]
  (draw-with ctx/world-viewport ctx/world-unit-scale render-fn))

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
                   ctx/camera))
