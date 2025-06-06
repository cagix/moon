(ns gdl.graphics.tiled-map-renderer
  (:require [gdl.tiled :as tiled])
  (:import (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn draw! [^OrthogonalTiledMapRenderer renderer tiled-map color-setter camera]
  (.setColorSetter renderer (reify ColorSetter
                              (apply [_ color x y]
                                (color-setter color x y))))
  (.setView renderer (:camera/java-object camera))
  ; there is also:
  ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
  ; but right order / visible only ?
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render renderer)))
