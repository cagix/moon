(ns cdq.graphics.tiled-map-renderer
  (:require [cdq.tiled :as tiled])
  (:import (cdq OrthogonalTiledMapRenderer ColorSetter)))

(defn create [tiled-map world-unit-scale batch]
  (OrthogonalTiledMapRenderer. tiled-map
                               (float world-unit-scale)
                               batch))

(defn draw! [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
  (.setColorSetter this (reify ColorSetter
                          (apply [_ color x y]
                            (color-setter color x y))))
  (.setView this camera)
  ; there is also:
  ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
  ; but right order / visible only ?
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render this)))
