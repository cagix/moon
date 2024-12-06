(ns forge.app.cursors
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [forge.core :refer [bind-root
                                defn-impl
                                safe-get
                                set-cursor
                                mapvals
                                dispose]]))

(defn- gdx-cursor [[file [hotspot-x hotspot-y]]]
  (let [pixmap (g/pixmap (files/internal (str "cursors/" file ".png")))
        cursor (g/cursor pixmap hotspot-x hotspot-y)]
    (dispose pixmap)
    cursor))

(declare ^:private cursors)

(defn create [[_ data]]
  (bind-root cursors (mapvals gdx-cursor data)))

(defn destroy [_]
  (run! dispose (vals cursors)))

(defn-impl set-cursor [cursor-key]
  (g/set-cursor (safe-get cursors cursor-key)))
