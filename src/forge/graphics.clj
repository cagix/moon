(ns forge.graphics
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.utils :as utils]
            [clojure.gdx.math :as math :refer [degree->radians]]
            [forge.assets :as assets]
            [forge.graphics.color]
            [forge.graphics.image :as image]
            [forge.graphics.text :as text]
            [forge.graphics.tiled :as tiled])
  (:import (com.badlogic.gdx.graphics OrthographicCamera Texture Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)))

(def delta-time        gdx/delta-time)
(def frames-per-second gdx/frames-per-second)

(defn clear-screen []
  (utils/clear-screen color/black))

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
         gui-viewport
         cursors)

(def ^:dynamic ^:private *unit-scale* 1)

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [^Viewport viewport]
  (let [mouse-x (math/clamp (gdx/mouse-x)
                            (.getLeftGutterWidth viewport)
                            (.getRightGutterX viewport))
        mouse-y (math/clamp (gdx/mouse-y)
                            (.getTopGutterHeight viewport)
                            (.getTopGutterY viewport))
        coords (.unproject viewport (math/v2 mouse-x mouse-y))]
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

(defn texture-region [path]
  (TextureRegion. ^Texture (assets/get path)))

(defn image [path]
  (image/create world-unit-scale (texture-region path)))

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

(defn- sd-color [color]
  (sd/set-color shape-drawer (forge.graphics.color/munge color)))

(defn draw-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/ellipse shape-drawer x y radius-x radius-y))

(defn draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (sd/filled-ellipse shape-drawer x y radius-x radius-y))

(defn draw-circle [[x y] radius color]
  (sd-color color)
  (sd/circle shape-drawer x y radius))

(defn draw-filled-circle [[x y] radius color]
  (sd-color color)
  (sd/filled-circle shape-drawer x y radius))

(defn draw-arc [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (sd/arc shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (sd/sector shape-drawer centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn draw-rectangle [x y w h color]
  (sd-color color)
  (sd/rectangle shape-drawer x y w h))

(defn draw-filled-rectangle [x y w h color]
  (sd-color color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn draw-line [[sx sy] [ex ey] color]
  (sd-color color)
  (sd/line shape-drawer sx sy ex ey))

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
  (let [old-line-width (sd/default-line-width shape-drawer)]
    (sd/set-default-line-width shape-drawer (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width shape-drawer old-line-width)))

(defn- draw-with [^Viewport viewport unit-scale draw-fn]
  (.setColor batch color/white) ; fix scene2d.ui.tooltip flickering
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

(defn- make-cursors [cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                   cursor (gdx/new-cursor pixmap hotspot-x hotspot-y)]
               (utils/dispose pixmap)
               cursor))
           cursors))

(defn set-cursor [cursor-key]
  (gdx/set-cursor (safe-get cursors cursor-key)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn init [{:keys [cursors]}]
  (bind-root #'batch (SpriteBatch.))
  (bind-root #'shape-drawer-texture (white-pixel-texture))
  (bind-root #'shape-drawer (sd/create batch (TextureRegion. shape-drawer-texture 1 0 1 1)))
  (bind-root #'default-font (text/truetype-font
                             {:file (gdx/internal-file "fonts/exocet/films.EXL_____.ttf")
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
                                      (tiled/renderer tiled-map world-unit-scale batch))))
  (bind-root #'gui-viewport (FitViewport. gui-viewport-width
                                          gui-viewport-height
                                          (OrthographicCamera.)))
  (bind-root #'cursors (make-cursors cursors)))

(defn dispose []
  (.dispose batch)
  (.dispose shape-drawer-texture)
  (.dispose default-font)
  (run! utils/dispose (vals cursors)))

(defn resize [w h]
  (.update gui-viewport   w h true)
  (.update world-viewport w h))
