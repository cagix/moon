(ns clojure.gdx.maps.tiled.tiled-map
  (:import (com.badlogic.gdx.maps.tiled TiledMap)))

(defn create []
  (TiledMap.))

(defn dispose! [^TiledMap tiled-map]
  (.dispose tiled-map))

(defn properties [^TiledMap tiled-map]
  (.getProperties tiled-map))

(defn layers [^TiledMap tiled-map]
  (.getLayers tiled-map))
