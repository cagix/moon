(ns forge.app.cursors
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.utils :refer [bind-root safe-get mapvals]]))

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

(defn set-cursor [cursor-key]
  (g/set-cursor (safe-get cursors cursor-key)))
