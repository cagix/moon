(ns clojure.gdx.maps.tiled.tiled-map-tile-layer
  (:require [clojure.gdx.maps.map-properties :as map-properties])
  (:import (com.badlogic.gdx.maps.tiled TiledMapTileLayer
                                        TiledMapTileLayer$Cell)))

(defn create
  [{:keys [width
           height
           tilewidth
           tileheight]}
   {:keys [name
           visible?
           properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [; tilewidth/tileheight should not be required as it it saved in the map
        ; in example `.tmx` file the layers do not have those properties
        ; but the constructor requires it.
        layer (TiledMapTileLayer. width height tilewidth tileheight)]
    (.setName layer name)
    (.setVisible layer visible?)
    (map-properties/add! (.getProperties layer) properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    layer))
