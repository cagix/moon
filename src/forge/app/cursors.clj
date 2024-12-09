(ns forge.app.cursors
  (:require [anvil.disposable :as disposable]
            [anvil.graphics :refer [cursors]]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as g]
            [clojure.utils :refer [bind-root mapvals]]))

(defn create [[_ data]]
  (bind-root cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                (let [pixmap (g/pixmap (files/internal (str "cursors/" file ".png")))
                                      cursor (g/cursor pixmap hotspot-x hotspot-y)]
                                  (disposable/dispose pixmap)
                                  cursor))
                              data)))

(defn dispose [_]
  (run! disposable/dispose (vals cursors)))
