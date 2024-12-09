(ns forge.app.cached-map-renderer
  (:require [anvil.graphics :as g]
            [anvil.tiled-map-renderer :as tiled-map-renderer]
            [clojure.utils :refer [bind-root]]))

(defn create [_]
  (bind-root g/cached-map-renderer (memoize (fn [tiled-map]
                                              (tiled-map-renderer/create tiled-map
                                                                         g/world-unit-scale
                                                                         g/batch)))))
