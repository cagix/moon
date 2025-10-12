(ns clojure.gdx.maps.map-layers
  (:refer-clojure :exclude [get])
  (:import (com.badlogic.gdx.maps MapLayers)))

(defn get [^MapLayers layers name]
  (.get layers ^String name))
