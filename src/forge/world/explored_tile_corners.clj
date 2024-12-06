(ns forge.world.explored-tile-corners
  (:require [clojure.gdx.tiled :as tiled]
            [data.grid2d :as g2d]
            [forge.utils :refer [bind-root]]))

(declare explored-tile-corners)

(defn init [tiled-map]
  (bind-root explored-tile-corners (atom (g2d/create-grid
                                          (tiled/tm-width  tiled-map)
                                          (tiled/tm-height tiled-map)
                                          (constantly false)))))
