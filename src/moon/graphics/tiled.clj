(ns moon.graphics.tiled
  (:require [gdl.graphics.tiled :as tiled]
            [moon.graphics :as g]
            [moon.graphics.world-view :as world-view]))

(declare ^:private cached-map-renderer)

(defn draw
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (tiled/render (cached-map-renderer tiled-map)
                color-setter
                (world-view/camera)
                tiled-map))

(defn- tiled-renderer [tiled-map]
  (tiled/renderer tiled-map (world-view/unit-scale) g/batch))

(defn init []
  (bind-root #'cached-map-renderer (memoize tiled-renderer)))
