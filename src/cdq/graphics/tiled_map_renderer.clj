(ns cdq.graphics.tiled-map-renderer
  (:require gdl.graphics.tiled-map-renderer
            [gdl.tiled :as tiled])
  (:import (gdl OrthogonalTiledMapRenderer ColorSetter)))

(defn create [{:keys [gdl.graphics/batch
                      gdl.graphics/world-unit-scale] :as context}]
  (assoc context :gdl.graphics/tiled-map-renderer
         (memoize (fn [tiled-map]
                    (OrthogonalTiledMapRenderer. tiled-map
                                                 (float world-unit-scale)
                                                 batch)))))

(extend-type OrthogonalTiledMapRenderer
  gdl.graphics.tiled-map-renderer/TiledMapRenderer
  (draw [this tiled-map color-setter camera]
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
