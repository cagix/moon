(ns forge.app.cached-map-renderer
  (:require [forge.system :as system :refer [bind-root defmethods]])
  (:import (forge OrthogonalTiledMapRenderer)))

(defmethods :app/cached-map-renderer
  (system/create [_]
    (bind-root #'system/cached-map-renderer
               (memoize
                (fn [tiled-map]
                  (OrthogonalTiledMapRenderer. tiled-map
                                               (float system/world-unit-scale)
                                               system/batch))))))
