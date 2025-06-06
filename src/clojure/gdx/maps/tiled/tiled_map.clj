(ns clojure.gdx.maps.tiled.tiled-map
  (:require [clojure.gdx.maps.tiled.tiled-map-tile-layer :as tiled-map-tile-layer])
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
