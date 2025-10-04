(ns com.badlogic.gdx.utils.viewport
  (:require [com.badlogic.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn camera [^Viewport this]
  (.getCamera this))

(defn world-width [^Viewport this]
  (.getWorldWidth this))

(defn world-height [^Viewport this]
  (.getWorldHeight this))

(defn unproject [^Viewport this x y]
  (-> this
      (.unproject (vector2/->java x y))
      vector2/->clj))

(defn update! [^Viewport viewport width height {:keys [center?]}]
  (.update viewport width height (boolean center?)))
