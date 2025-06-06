(ns clojure.gdx.maps.tiled.tmx-map-loader
  (:import (com.badlogic.gdx.maps.tiled TmxMapLoader)))

(defn load! [file-name]
  (.load (TmxMapLoader.) file-name))
