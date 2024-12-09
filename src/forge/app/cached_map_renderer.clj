(ns forge.app.cached-map-renderer
  (:require [anvil.graphics :refer [batch cached-map-renderer]]
            [clojure.utils :refer [bind-root]]
            [forge.app.world-viewport :refer [world-unit-scale]])
  (:import (forge OrthogonalTiledMapRenderer)))

(defn create [_]
  (bind-root cached-map-renderer
             (memoize
              (fn [tiled-map]
                (OrthogonalTiledMapRenderer. tiled-map
                                             (float world-unit-scale)
                                             batch)))))
