(ns forge.graphics
  (:require [forge.graphics.color :as color])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn delta-time        [] (.getDeltaTime       Gdx/graphics))
(defn frames-per-second [] (.getFramesPerSecond Gdx/graphics))

(defn clear-screen [color]
  (ScreenUtils/clear (color/munge color)))

(defn cursor [file [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (.internal Gdx/files file))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))
