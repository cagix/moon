(ns cdq.world.tiled-map
  (:require [clojure.gdx.maps.map-layers :as layers]
            [clojure.gdx.maps.map-properties :as props]
            [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.maps.tiled.tiled-map-tile :as tile]
            [clojure.gdx.maps.tiled.layer :as layer]
            [clojure.gdx.maps.tiled.layer.cell :as cell]))

(defn spawn-positions
  [tiled-map]
  (let [layer-name "creatures"
        property-key "id"
        layer (layers/get (tiled-map/layers tiled-map) layer-name)]
    (for [x (range (layer/width layer))
          y (range (layer/height layer))
          :let [position [x y]
                cell (layer/cell layer position)]
          :when cell
          :let [value (-> cell
                          cell/tile
                          tile/properties
                          (props/get property-key))]
          :when value]
      [position value])))
