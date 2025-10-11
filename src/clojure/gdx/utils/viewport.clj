(ns clojure.gdx.utils.viewport
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn camera [this]
  (.getCamera this))

(defn world-width [this]
  (.getWorldWidth this))

(defn world-height [this]
  (.getWorldHeight this))

(defn update! [viewport width height {:keys [center?]}]
  (.update viewport width height (boolean center?)))

(defn unproject [viewport vector2]
  (.unproject viewport vector2))
