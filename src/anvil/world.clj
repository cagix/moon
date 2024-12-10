(ns anvil.world
  (:require [anvil.graphics :as g]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.viewport :as vp])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(declare unit-scale
         viewport-width
         viewport-height
         viewport)

(defn pixels->units [pixels]
  (* (int pixels) unit-scale))

(defn mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (vp/unproject-mouse-position viewport))

(defn camera []
  (vp/camera viewport))

(defn draw-on-view [render-fn]
  (g/draw-with viewport unit-scale render-fn))

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

(declare tiled-map-renderer)

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (draw-tiled-map* (tiled-map-renderer tiled-map)
                   tiled-map
                   color-setter
                   (camera)))
