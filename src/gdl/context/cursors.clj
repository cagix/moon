(ns gdl.context.cursors
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [gdl.context :as ctx]))

(defn setup [cursors]
  (bind-root ctx/cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                          (let [pixmap (pixmap/create (files/internal gdx/files (str "cursors/" file ".png")))
                                cursor (g/cursor gdx/graphics pixmap hotspot-x hotspot-y)]
                            (dispose pixmap)
                            cursor))
                        cursors)))

(defn cleanup []
  (run! dispose (vals ctx/cursors)))
