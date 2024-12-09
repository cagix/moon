(ns forge.world.raycaster
  (:require [anvil.world :as world :refer [blocks-vision?]]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [bind-root]]
            [data.grid2d :as g2d]))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- init-raycaster [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    (bind-root world/raycaster [arr width height])))

(defn init [tiled-map]
  (init-raycaster world/grid blocks-vision?))
