(ns cdq.graphics.tm-renderer
  (:require [clojure.color :as color]
            [clojure.gdx.maps.map-layers :as layers]
            [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.maps.tiled.layer :as layer]
            [clojure.gdx.utils.viewport :as viewport])
  (:import (cdq.graphics ColorSetter
                         TiledMapRenderer)))

(defn draw! [tiled-map-renderer world-viewport tiled-map color-setter]
  (let [^TiledMapRenderer renderer (tiled-map-renderer tiled-map)
        camera (viewport/camera world-viewport)]
    (.setColorSetter renderer (reify ColorSetter
                                (apply [_ color x y]
                                  (color/float-bits (color-setter color x y)))))
    (.setView renderer camera)
    (->> tiled-map
         tiled-map/layers
         (filter layer/visible?)
         (map (partial layers/get-index (tiled-map/layers tiled-map)))
         int-array
         (.render renderer))))

(defn create [world-unit-scale batch]
  (memoize (fn [tiled-map]
             (TiledMapRenderer. tiled-map (float world-unit-scale) batch))))
