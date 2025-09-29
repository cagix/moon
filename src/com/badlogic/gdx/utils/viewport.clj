(ns com.badlogic.gdx.utils.viewport
  (:require [com.badlogic.gdx.math.vector2 :as vector2])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn unproject [^Viewport viewport x y]
  (-> viewport
      (.unproject (vector2/->java x y))
      vector2/->clj))

(defn update! [^Viewport viewport width height & {:keys [center?]}]
  (.update viewport width height (boolean center?)))
