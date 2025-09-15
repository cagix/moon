(ns clojure.gdx.maps.tiled.layer
  (:import (com.badlogic.gdx.maps.tiled TiledMapTileLayer
                                        TiledMapTileLayer$Cell)))

(defn create
  [{:keys [width
           height
           tilewidth
           tileheight
           name
           visible?
           map-properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [layer (doto (TiledMapTileLayer. width height tilewidth tileheight)
                (.setName name)
                (.setVisible visible?))]
    (.putAll (.getProperties layer) map-properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    layer))
