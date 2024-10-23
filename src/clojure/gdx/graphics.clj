(ns clojure.gdx.graphics
  (:require [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn delta-time        [] (.getDeltaTime       Gdx/graphics))
(defn frames-per-second [] (.getFramesPerSecond Gdx/graphics))

(defn clear-screen [color]
  (ScreenUtils/clear (color/munge color)))
