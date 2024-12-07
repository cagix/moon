(ns forge.world.tiled-map
  (:require [clojure.utils :refer [bind-root]]))

(declare world-tiled-map)

(defn init [tiled-map]
  (bind-root world-tiled-map tiled-map))
