(ns cdq.create.tiled-map-renderer
  (:import (gdl OrthogonalTiledMapRenderer)))

(defn create [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))
