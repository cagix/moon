(ns clojure.gdx.maps.tiled.tiled-map-tile-layer
  (:refer-clojure :exclude [name])
  (:import (com.badlogic.gdx.maps MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMapTileLayer
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

(defn properties ^MapProperties [^TiledMapTileLayer layer]
  (.getProperties layer))

(defn get-cell ^TiledMapTileLayer$Cell [^TiledMapTileLayer layer [x y]]
  (.getCell layer x y))

(defn set-visible! [^TiledMapTileLayer layer boolean]
  (.setVisible layer boolean))

(defn visible? [^TiledMapTileLayer layer]
  (.isVisible layer))

(defn name [^TiledMapTileLayer layer]
  (.getName layer))
