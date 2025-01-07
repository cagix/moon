(ns gdl.context.tiled-map-renderer
  (:import (gdl OrthogonalTiledMapRenderer)))

(defn create [_ {:keys [gdl.context/world-unit-scale
                        gdl.context/batch]}]
  (assert world-unit-scale)
  (assert batch)
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))
