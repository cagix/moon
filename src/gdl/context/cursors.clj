(ns gdl.context.cursors
  (:require [clojure.gdx :as gdx]
            [clojure.utils :refer [mapvals]]))

(defn create [[_ cursors] c]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (gdx/pixmap (gdx/internal-file c (str "cursors/" file ".png")))
                   cursor (gdx/cursor c pixmap hotspot-x hotspot-y)]
               (gdx/dispose pixmap)
               cursor))
           cursors))

(defn dispose [[_ cursors]]
  (run! gdx/dispose (vals cursors)))
