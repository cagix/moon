(ns ^:no-doc forge.app.cached-map-renderer
  (:require [forge.core :refer :all])
  (:import (forge OrthogonalTiledMapRenderer)))

(defmethods :app/cached-map-renderer
  (app-create [_]
    (bind-root #'cached-map-renderer
               (memoize
                (fn [tiled-map]
                  (OrthogonalTiledMapRenderer. tiled-map
                                               (float world-unit-scale)
                                               batch))))))
