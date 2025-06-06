(ns clojure.gdx.maps.tiled.tiled-map-tile-layer
  (:require [clojure.gdx.maps.map-properties :as map-properties])
  (:import (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell)))

(defn add!
  "Returns nil."
  [^TiledMap tiled-map
   {:keys [name
           visible?
           properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [tm-props (.getProperties tiled-map)
        layer (TiledMapTileLayer. (.get tm-props "width")
                                  (.get tm-props "height")
                                  (.get tm-props "tilewidth")
                                  (.get tm-props "tileheight"))]
    (.setName layer name)
    (.setVisible layer visible?)
    (map-properties/add! (.getProperties layer) properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    (.add (.getLayers tiled-map) layer)
    nil))
