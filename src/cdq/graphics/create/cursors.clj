(ns cdq.graphics.create.cursors
  (:require [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]))

(defn create
  [{:keys [graphics/core]
    :as graphics}
   cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (pixmap/create file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (pixmap/dispose! pixmap)
                                                     cursor)))))
