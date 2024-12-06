(ns forge.app.cached-map-renderer
  (:require [clojure.gdx.tiled :as tiled]
            [forge.core :refer [world-unit-scale
                                world-camera
                                batch]]
            [forge.utils :refer [bind-root]])
  (:import (forge OrthogonalTiledMapRenderer
                  ColorSetter)))

(declare ^:private cached-map-renderer)

(defn create [_]
  (bind-root cached-map-renderer
             (memoize
              (fn [tiled-map]
                (OrthogonalTiledMapRenderer. tiled-map
                                             (float world-unit-scale)
                                             batch)))))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (let [^OrthogonalTiledMapRenderer map-renderer (cached-map-renderer tiled-map)]
    (.setColorSetter map-renderer (reify ColorSetter
                                    (apply [_ color x y]
                                      (color-setter color x y))))
    (.setView map-renderer (world-camera))
    (->> tiled-map
         tiled/layers
         (filter tiled/visible?)
         (map (partial tiled/layer-index tiled-map))
         int-array
         (.render map-renderer))))
