(ns forge.app.cursors
  (:require [forge.core :refer [bind-root
                                cursors
                                mapvals
                                dispose]])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn- gdx-cursor [[file [hotspot-x hotspot-y]]]
  (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (dispose pixmap)
    cursor))

(defn create [[_ data]]
  (bind-root cursors (mapvals gdx-cursor data)))

(defn destroy [_]
  (run! dispose (vals cursors)))
