(ns cdq.create.cursors
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]))

(defn do!
  [ctx
   {:keys [data
           path-format]}]
  (assoc ctx :ctx/cursors (update-vals data
                                       (fn [[file [hotspot-x hotspot-y]]]
                                         (let [pixmap (pixmap/create (files/internal (gfx/files) (format path-format file)))
                                               cursor (graphics/cursor (gdx/graphics) pixmap hotspot-x hotspot-y)]
                                           (.dispose pixmap)
                                           cursor)))))
