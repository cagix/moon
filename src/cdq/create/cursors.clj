(ns cdq.create.cursors
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]))

(defn- load-cursors [files graphics cursors cursor-path-format]
  (update-vals cursors
               (fn [[file [hotspot-x hotspot-y]]]
                 (let [pixmap (pixmap/create (files/internal files (format cursor-path-format file)))
                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                   (.dispose pixmap)
                   cursor))))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (assoc ctx :ctx/cursors (load-cursors (gdx/files) (gdx/graphics) (:cursors config) (:cursor-path-format config))))
