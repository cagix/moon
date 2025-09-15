(ns clojure.gdx.maps.tiled.layer
  (:require [clojure.gdx.maps.properties :as properties])
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

(defn get-property [^TiledMapTileLayer layer k]
  (.get (.getProperties layer) k))

(defn map-properties [^TiledMapTileLayer layer]
  (properties/->clj (.getProperties layer)))

(defn set-visible! [^TiledMapTileLayer layer boolean]
  (.setVisible layer boolean))

(defn visible? [^TiledMapTileLayer layer]
  (.isVisible layer))

(defn layer-name [^TiledMapTileLayer layer]
  (.getName layer))

(defn tile-at [^TiledMapTileLayer layer [x y]]
  (when-let [cell (.getCell layer x y)]
    (.getTile cell)))

(defn property-value [^TiledMapTileLayer layer [x y] property-key]
  (if-let [cell (.getCell layer x y)]
    (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
      value
      :undefined)
    :no-cell))
