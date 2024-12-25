(ns gdl.context
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as font]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.input :as input]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.math.utils :refer [clamp degree->radians]]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.string :as str]
            [gdl.graphics.sprite :as sprite]
            [gdl.tiled :as tiled])
  (:import (com.badlogic.gdx Gdx)
           (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn get-sound [{::keys [assets]} sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))

(defn play-sound [c sound-name]
  (sound/play (get-sound c sound-name)))

(declare world-unit-scale
         world-viewport-width
         world-viewport-height
         world-viewport
         camera)

(defn- texture-region [{::keys [assets]} path]
  (texture-region/create (assets path)))

(defn sprite [c path]
  (sprite/create world-unit-scale
                 (texture-region c path)))

(defn sub-sprite [sprite xywh]
  (sprite/sub world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [c path tilew tileh]
  (sprite/sheet world-unit-scale
                (texture-region c path)
                tilew
                tileh))

(defn from-sprite-sheet [sprite-sheet xy]
  (sprite/from-sheet world-unit-scale
                     sprite-sheet
                     xy))

(declare default-font)

(declare cursors)

(defn set-cursor [cursor-key]
  (g/set-cursor Gdx/graphics (safe-get cursors cursor-key)))

(declare viewport
         viewport-width
         viewport-height)

(declare tiled-map-renderer)

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

(declare shape-drawer)

(def ^:dynamic *unit-scale* 1)

(declare assets)

(declare batch)

(defn get-ctx []
  {::assets       assets
   ::default-font default-font
   ::batch        batch
   ::unit-scale   *unit-scale*})

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply color/create c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- sd-color [color]
  (sd/set-color shape-drawer (munge-color color)))

(defn ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/ellipse shape-drawer x y radius-x radius-y))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/filled-ellipse shape-drawer x y radius-x radius-y))

(defn circle [[x y] radius color]
  (sd-color color)
  (sd/circle shape-drawer x y radius))

(defn filled-circle [[x y] radius color]
  (sd-color color)
  (sd/filled-circle shape-drawer x y radius))

(defn arc [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (sd/arc shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [[center-x center-y] radius start-angle degree color]
  (sd-color color)
  (sd/sector shape-drawer center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [x y w h color]
  (sd-color color)
  (sd/rectangle shape-drawer x y w h))

(defn filled-rectangle [x y w h color]
  (sd-color color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn line [[sx sy] [ex ey] color]
  (sd-color color)
  (sd/line shape-drawer sx sy ex ey))

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
  (let [old-line-width (sd/default-line-width shape-drawer)]
    (sd/set-default-line-width shape-drawer (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width shape-drawer old-line-width)))

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
  [{::keys [default-font batch unit-scale]}
   {:keys [font x y text h-align up? scale]}]
  (let [font (or font default-font)
        data (font/data font)
        old-scale (float (font/scale-x data))]
    (font/set-scale data (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
    (font/draw :font font
               :batch batch
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
  (if color (batch/set-color batch color/white)))

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
  (batch/set-color batch color/white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (draw-fn)
  (batch/end batch))

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
  [viewport]
  (let [mouse-x (clamp (input/x Gdx/input)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (input/y Gdx/input)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (viewport/unproject viewport mouse-x mouse-y)))

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position viewport)))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport world-unit-scale render-fn))
