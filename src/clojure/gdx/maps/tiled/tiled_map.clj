(ns clojure.gdx.maps.tiled.tiled-map
  (:require [clojure.gdx.maps.map-properties :as map-properties]
            [clojure.gdx.maps.tiled.tiled-map-tile-layer :as tiled-map-tile-layer])
  (:import (com.badlogic.gdx.maps.tiled TiledMap)))

(defn add-layer!
  "Returns nil."
  [^TiledMap tiled-map layer-declaration]
  (let [props (.getProperties tiled-map)
        layer (tiled-map-tile-layer/create {:width      (.get props "width")
                                            :height     (.get props "height")
                                            :tilewidth  (.get props "tilewidth")
                                            :tileheight (.get props "tileheight")}
                                           layer-declaration)]
    (.add (.getLayers tiled-map) layer))
  nil)

(defn create [{:keys [properties
                      layers]}]
  (let [tiled-map (TiledMap.)]
    (map-properties/add! (.getProperties tiled-map) properties)
    (doseq [layer layers]
      (add-layer! tiled-map layer))
    tiled-map))
