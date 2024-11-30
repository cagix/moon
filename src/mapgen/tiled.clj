(ns mapgen.tiled
  (:require [data.grid2d :as g]
            [forge.tiled :as t]))

(defn grid->tiled-map
  "Creates an empty new tiled-map with same layers and properties as schema-tiled-map.
  The size of the map is as of the grid, which contains also the tile information from the schema-tiled-map."
  [schema-tiled-map grid]
  (let [tiled-map (t/empty-tiled-map)
        properties (t/m-props tiled-map)]
    (t/put-all! properties (t/m-props schema-tiled-map))
    (t/put! properties "width"  (g/width  grid))
    (t/put! properties "height" (g/height grid))
    (doseq [layer (t/layers schema-tiled-map)
            :let [new-layer (t/add-layer! tiled-map
                                          :name (t/layer-name layer)
                                          :visible (t/visible? layer)
                                          :properties (t/m-props layer))]]
      (doseq [position (g/posis grid)
              :let [local-position (get grid position)]
              :when local-position]
        (when (vector? local-position)
          (when-let [cell (t/cell-at schema-tiled-map layer local-position)]
            (t/set-tile! new-layer
                         position
                         (t/copy-tile (t/cell->tile cell)))))))
    tiled-map))

(defn wgt-grid->tiled-map [tile-size grid position->tile]
  (let [tiled-map (t/empty-tiled-map)
        properties (t/m-props tiled-map)]
    (t/put! properties "width"  (g/width  grid))
    (t/put! properties "height" (g/height grid))
    (t/put! properties "tilewidth"  tile-size)
    (t/put! properties "tileheight" tile-size)
    (let [layer (t/add-layer! tiled-map :name "ground" :visible true)
          properties (t/m-props layer)]
      (t/put! properties "movement-properties" true)
      (doseq [position (g/posis grid)
              :let [value (get grid position)
                    cell (t/cell-at tiled-map layer position)]]
        (t/set-tile! layer position (position->tile position))))
    tiled-map))
