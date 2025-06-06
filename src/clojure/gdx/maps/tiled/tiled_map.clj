(ns clojure.gdx.maps.tiled.tiled-map
  (:import (com.badlogic.gdx.maps MapLayers
                                  MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMap)))

(defn create []
  (TiledMap.))

(defn dispose! [^TiledMap tiled-map]
  (.dispose tiled-map))

(defn properties ^MapProperties [^TiledMap tiled-map]
  (.getProperties tiled-map))

(defn layers ^MapLayers [^TiledMap tiled-map]
  (.getLayers tiled-map))
