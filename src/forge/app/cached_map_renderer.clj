(ns forge.app.cached-map-renderer
  (:require [clojure.gdx.tiled :as tiled]
            [forge.core :refer [defn-impl
                                world-unit-scale
                                world-camera
                                batch
                                draw-tiled-map]]
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

(defn-impl draw-tiled-map [tiled-map color-setter]
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
