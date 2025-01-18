(ns cdq.graphics.tiled-map-renderer
  (:require [cdq.tiled :as tiled])
  (:import (gdl OrthogonalTiledMapRenderer ColorSetter)))

(defn create [{:keys [cdq.graphics/batch
                      cdq.graphics/world-unit-scale]}]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- draw* [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
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

(defn draw
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [{:keys [cdq.graphics/tiled-map-renderer
           cdq.graphics/world-viewport]}
   tiled-map
   color-setter]
  (draw* (tiled-map-renderer tiled-map)
         tiled-map
         color-setter
         (:camera world-viewport)))
