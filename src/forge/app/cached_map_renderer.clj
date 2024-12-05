(ns forge.app.cached-map-renderer
  (:require [forge.core :refer [bind-root
                                cached-map-renderer
                                world-unit-scale
                                batch]])
  (:import (forge OrthogonalTiledMapRenderer)))

(defn create []
  (bind-root #'cached-map-renderer
             (memoize
              (fn [tiled-map]
                (OrthogonalTiledMapRenderer. tiled-map
                                             (float world-unit-scale)
                                             batch)))))
