(ns cdq.graphics.tm-renderer
  (:require [clojure.color :as color]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.viewport :as viewport])
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
         tiled/layers
         (filter tiled/visible?)
         (map (partial tiled/layer-index tiled-map))
         int-array
         (.render renderer))))

(defn create [world-unit-scale batch]
  (memoize (fn [tiled-map]
             (TiledMapRenderer. (:tiled-map/java-object tiled-map)
                                (float world-unit-scale)
                                batch))))
