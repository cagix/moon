(ns anvil.tiled-map-renderer
  (:require [clojure.gdx.tiled :as tiled])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn create [tiled-map unit-scale batch]
  (OrthogonalTiledMapRenderer. tiled-map
                               (float unit-scale)
                               batch))

(defn render
  [^OrthogonalTiledMapRenderer renderer tiled-map color-setter camera]
  (.setColorSetter renderer (reify ColorSetter
                              (apply [_ color x y]
                                (color-setter color x y))))
  (.setView renderer camera)
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render renderer)))
