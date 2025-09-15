(ns cdq.gdx.graphics
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.graphics.color :as color]
            [clojure.utils :as utils]))

(defn dispose!
  [{:keys [ctx/batch
           ctx/cursors
           ctx/default-font
           ctx/shape-drawer-texture
           ctx/textures]}]
  (disposable/dispose! batch)
  (run! disposable/dispose! (vals cursors))
  (disposable/dispose! default-font)
  (disposable/dispose! shape-drawer-texture)
  (run! disposable/dispose! (vals textures)))

(defn draw-arc!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))

(defn draw-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defn draw-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defn draw-filled-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defn draw-line!
  [{:keys [ctx/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defn draw-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defn draw-sector!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (utils/degree->radians start-angle)
              (utils/degree->radians degree)))

(defn draw-text!
  [{:keys [ctx/batch
           ctx/unit-scale
           ctx/default-font]}
   {:keys [font scale x y text h-align up?]}]
  (bitmap-font/draw! (or font default-font)
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

(defn with-line-width
  [{:keys [ctx/shape-drawer]} width f]
  (sd/with-line-width shape-drawer width f))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
   f]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1)))
  (batch/end! batch))

(defn draw-tiled-map!
  [{:keys [ctx/tiled-map-renderer
           ctx/world-viewport]}
   tiled-map
   color-setter]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     tiled-map
                     color-setter))

(defn set-cursor!
  [{:keys [ctx/cursors
           ctx/graphics]}
   cursor-key]
  (assert (contains? cursors cursor-key))
  (graphics/set-cursor! graphics (get cursors cursor-key)))

(defn delta-time
  [{:keys [ctx/graphics]}]
  (graphics/delta-time graphics))

(defn frames-per-second
  [{:keys [ctx/graphics]}]
  (graphics/frames-per-second graphics))

(def world-viewport-width  (comp :viewport/width  :ctx/world-viewport))
(def world-viewport-height (comp :viewport/height :ctx/world-viewport))

(def camera-position      (comp :camera/position     :viewport/camera :ctx/world-viewport))
(def visible-tiles        (comp camera/visible-tiles :viewport/camera :ctx/world-viewport))
(def camera-frustum       (comp camera/frustum       :viewport/camera :ctx/world-viewport))
(def camera-zoom          (comp :camera/zoom         :viewport/camera :ctx/world-viewport))

(defn change-zoom! [{:keys [ctx/world-viewport]} amount]
  (camera/inc-zoom! (:viewport/camera world-viewport) amount))

(defn set-camera-position! [{:keys [ctx/world-viewport]} position]
  (camera/set-position! (:viewport/camera world-viewport) position))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn- unproject-clamp [viewport [x y]]
  (viewport/unproject viewport
                      (utils/clamp x
                                   (:viewport/left-gutter-width viewport)
                                   (:viewport/right-gutter-x    viewport))
                      (utils/clamp y
                                   (:viewport/top-gutter-height viewport)
                                   (:viewport/top-gutter-y      viewport))))

(def unproject-ui    (fn [{:keys [ctx/ui-viewport]}    position] (unproject-clamp ui-viewport    position)))
(def unproject-world (fn [{:keys [ctx/world-viewport]} position] (unproject-clamp world-viewport position)))

(defn update-viewports!
  [{:keys [ctx/ui-viewport
           ctx/world-viewport]}
   width height]
  (viewport/update! ui-viewport    width height :center? true)
  (viewport/update! world-viewport width height :center? false))

(defn texture-region
  [{:keys [ctx/textures]}
   {:keys [image/file image/bounds]}]
  (assert file)
  (assert (contains? textures file))
  (let [texture (get textures file)]
    (if bounds
      (texture/region texture bounds)
      (texture/region texture))))

(defn clear! [{:keys [ctx/graphics]} [r g b a]]
  (graphics/clear! graphics r g b a))
