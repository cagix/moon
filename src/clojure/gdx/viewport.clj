(ns clojure.gdx.viewport
  (:require [clojure.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn camera [^Viewport this]
  (.getCamera this))

(defn update! [^Viewport viewport width height {:keys [center?]}]
  (.update viewport width height (boolean center?)))

(defn world-width [^Viewport this]
  (.getWorldWidth this))

(defn world-height [^Viewport this]
  (.getWorldHeight this))

(defn unproject [^Viewport viewport [x y]]
  (-> viewport
      (.unproject (vector2/->java x y))
      vector2/->clj))
