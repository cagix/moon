(ns forge.app.cursors
  (:require [forge.core :refer [bind-root
                                defn-impl
                                safe-get
                                set-cursor
                                mapvals
                                dispose]])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn- gdx-cursor [[file [hotspot-x hotspot-y]]]
  (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (dispose pixmap)
    cursor))

(declare ^:private cursors)

(defn create [[_ data]]
  (bind-root cursors (mapvals gdx-cursor data)))

(defn destroy [_]
  (run! dispose (vals cursors)))

(defn-impl set-cursor [cursor-key]
  (.setCursor Gdx/graphics (safe-get cursors cursor-key)))
