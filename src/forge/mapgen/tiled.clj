(ns forge.mapgen.tiled
  (:require [data.grid2d :as g]
            [forge.core :refer :all]))

(defn grid->tiled-map
  "Creates an empty new tiled-map with same layers and properties as schema-tiled-map.
  The size of the map is as of the grid, which contains also the tile information from the schema-tiled-map."
  [schema-tiled-map grid]
  (let [tiled-map (empty-tiled-map)
        properties (m-props tiled-map)]
    (put-all! properties (m-props schema-tiled-map))
    (put! properties "width"  (g/width  grid))
    (put! properties "height" (g/height grid))
    (doseq [layer (layers schema-tiled-map)
            :let [new-layer (add-layer! tiled-map
                                          :name (layer-name layer)
                                          :visible (visible? layer)
                                          :properties (m-props layer))]]
      (doseq [position (g/posis grid)
              :let [local-position (get grid position)]
              :when local-position]
        (when (vector? local-position)
          (when-let [cell (cell-at schema-tiled-map layer local-position)]
            (set-tile! new-layer
                         position
                         (copy-tile (cell->tile cell)))))))
    tiled-map))

(defn wgt-grid->tiled-map [tile-size grid position->tile]
  (let [tiled-map (empty-tiled-map)
        properties (m-props tiled-map)]
    (put! properties "width"  (g/width  grid))
    (put! properties "height" (g/height grid))
    (put! properties "tilewidth"  tile-size)
    (put! properties "tileheight" tile-size)
    (let [layer (add-layer! tiled-map :name "ground" :visible true)
          properties (m-props layer)]
      (put! properties "movement-properties" true)
      (doseq [position (g/posis grid)
              :let [value (get grid position)
                    cell (cell-at tiled-map layer position)]]
        (set-tile! layer position (position->tile position))))
    tiled-map))
