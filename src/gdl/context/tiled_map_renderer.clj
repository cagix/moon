(ns gdl.context.tiled-map-renderer
  (:require [gdl.context :as ctx])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(defn setup [world-unit-scale batch]
  (bind-root ctx/tiled-map-renderer
             (memoize (fn [tiled-map]
                        (OrthogonalTiledMapRenderer. tiled-map
                                                     (float world-unit-scale)
                                                     batch)))))
