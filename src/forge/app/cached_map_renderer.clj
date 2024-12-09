(ns forge.app.cached-map-renderer
  (:require [anvil.graphics :as g]
            [anvil.tiled-map-renderer :refer [TiledMapRenderer]]
            [clojure.gdx.tiled :as tiled]
            [clojure.utils :refer [bind-root]])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn create [_]
  (bind-root g/cached-map-renderer
             (memoize (fn [tiled-map]
                        (OrthogonalTiledMapRenderer. tiled-map
                                                     (float g/world-unit-scale)
                                                     g/batch)))))

(extend-type OrthogonalTiledMapRenderer
  TiledMapRenderer
  (render [this tiled-map color-setter camera]
    (.setColorSetter this (reify ColorSetter
                            (apply [_ color x y]
                              (color-setter color x y))))
    (.setView this camera)
    (->> tiled-map
         tiled/layers
         (filter tiled/visible?)
         (map (partial tiled/layer-index tiled-map))
         int-array
         (.render this))))
