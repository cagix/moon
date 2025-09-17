(ns cdq.world-fns.modules.convert-to-tiled-map
  (:require [clojure.grid2d :as g2d]
            [clojure.gdx.maps.tiled]
            [clojure.gdx.maps.tiled.tiles.static-tiled-map-tile :as static-tiled-map-tile]
            [clojure.tiled :as tiled]))

(defn- grid->tiled-map
  "Creates an empty new tiled-map with same layers and properties as schema-tiled-map.
  The size of the map is as of the grid, which contains also the tile information from the schema-tiled-map."
  [schema-tiled-map grid]
  (clojure.gdx.maps.tiled/create-tiled-map
   {:properties (merge (tiled/map-properties schema-tiled-map)
                       {"width" (g2d/width grid)
                        "height" (g2d/height grid)})
    :layers (for [layer (tiled/layers schema-tiled-map)]
              {:name (tiled/layer-name layer)
               :visible? (tiled/visible? layer)
               :properties (tiled/map-properties layer)
               :tiles (for [position (g2d/posis grid)
                            :let [local-position (get grid position)]
                            :when local-position]
                        (when (vector? local-position)
                          (when-let [tile (tiled/tile-at layer local-position)]
                            [position (static-tiled-map-tile/copy tile)])))})}))

(defn do!
  [{:keys [scaled-grid
           schema-tiled-map]
    :as w}]
  (assoc w :tiled-map (grid->tiled-map schema-tiled-map scaled-grid)))
