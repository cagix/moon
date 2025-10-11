(ns cdq.world.tiled-map
  (:require [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.maps.tiled.layer :as layer]))

(defn spawn-positions
  [tiled-map]
  (let [layer-name "creatures"
        property-key "id"
        layer (.get (tiled-map/layers tiled-map) layer-name)]
    (for [x (range (layer/width layer))
          y (range (layer/height layer))
          :let [position [x y]
                cell (layer/cell layer position)]
          :when cell
          :let [value (.get (.getProperties (.getTile cell)) property-key)]
          :when value]
      [position value])))
