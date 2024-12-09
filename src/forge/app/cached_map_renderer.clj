(ns forge.app.cached-map-renderer
  (:require [anvil.graphics :refer [batch cached-map-renderer world-unit-scale]]
            [clojure.utils :refer [bind-root]])
  (:import (forge OrthogonalTiledMapRenderer)))

(defn create [_]
  (bind-root cached-map-renderer
             (memoize
              (fn [tiled-map]
                (OrthogonalTiledMapRenderer. tiled-map
                                             (float world-unit-scale)
                                             batch)))))
