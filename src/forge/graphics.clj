(ns forge.graphics
  (:require [forge.assets :as assets]
            [forge.graphics.image :as image]
            [forge.graphics.shape-drawer :as sd]
            [forge.graphics.text :as text]
            [forge.graphics.tiled :as tiled]
            [forge.graphics.viewport :as vp]
            [forge.utils :refer [dispose mapvals]])
  (:import (com.badlogic.gdx.graphics Color OrthographicCamera Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)))

(def tile-size 48)

(def world-viewport-width 1440)
(def world-viewport-height 900)

(def gui-viewport-width 1440)
(def gui-viewport-height 900)

(declare batch
         ^:private shape-drawer
         ^:private shape-drawer-texture
         ^:private default-font
         ^:private cached-map-renderer
         ^:private world-unit-scale
         world-viewport
         gui-viewport)

(def ^:dynamic ^:private *unit-scale* 1)

(defn gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-position gui-viewport)))

(defn pixels->world-units [pixels]
  (* (int pixels) world-unit-scale))

(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (vp/unproject-mouse-position world-viewport))

(defn world-camera []
  (.getCamera world-viewport))

(defn image [path]
  (image/create world-unit-scale
                (assets/texture-region path)))

(defn sub-image [image bounds]
  (image/sub-image world-unit-scale
                   image
                   bounds))

(defn sprite-sheet [path tilew tileh]
  {:image (image path)
   :tilew tilew
   :tileh tileh})

(defn sprite [sprite-sheet index]
  (image/sprite world-unit-scale
                sprite-sheet
                index))

(defn draw-text [opts]
  (text/draw batch *unit-scale* default-font opts))

(defn draw-image [image position]
  (image/draw batch *unit-scale* image position))

(defn draw-centered [image position]
  (image/draw-centered batch *unit-scale* image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch *unit-scale* image rotation position))

(defn draw-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/ellipse shape-drawer position radius-x radius-y))

(defn draw-filled-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/filled-ellipse shape-drawer position radius-x radius-y))

(defn draw-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/circle shape-drawer position radius))

(defn draw-filled-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/filled-circle shape-drawer position radius))

(defn draw-arc [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/arc shape-drawer center radius start-angle degree))

(defn draw-sector [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/sector shape-drawer center radius start-angle degree))

(defn draw-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/rectangle shape-drawer x y w h))

(defn draw-filled-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn draw-line [start end color]
  (sd/set-color shape-drawer color)
  (sd/line shape-drawer start end))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd/set-color shape-drawer color)
  (sd/grid shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defn with-line-width [width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defn- draw-with [^Viewport viewport unit-scale draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
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
  (tiled/render (cached-map-renderer tiled-map)
                color-setter
                (world-camera)
                tiled-map))

(defn init []
  (.bindRoot #'batch (SpriteBatch.))
  (.bindRoot #'shape-drawer-texture (sd/white-pixel-texture))
  (.bindRoot #'shape-drawer (sd/create batch shape-drawer-texture))
  (.bindRoot #'default-font (text/truetype-font
                             {:file "fonts/exocet/films.EXL_____.ttf"
                              :size 16
                              :quality-scaling 2}))
  (.bindRoot #'world-unit-scale (float (/ tile-size)))
  (.bindRoot #'world-viewport (let [world-width  (* world-viewport-width world-unit-scale)
                                    world-height (* world-viewport-height world-unit-scale)
                                    camera (OrthographicCamera.)
                                    y-down? false]
                                (.setToOrtho camera y-down? world-width world-height)
                                (FitViewport. world-width world-height camera)))
  (.bindRoot #'cached-map-renderer (memoize
                                    (fn [tiled-map]
                                      (tiled/renderer tiled-map world-unit-scale batch))))
  (.bindRoot #'gui-viewport (FitViewport. gui-viewport-width
                                          gui-viewport-height
                                          (OrthographicCamera.))))

(defn dispose []
  (.dispose batch)
  (.dispose shape-drawer-texture)
  (.dispose default-font))
