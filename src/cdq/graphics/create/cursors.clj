(ns cdq.graphics.create.cursors
  (:require [clojure.gdx.graphics :as graphics])
  (:import (com.badlogic.gdx.graphics Pixmap)))

(defn create
  [{:keys [graphics/core]
    :as graphics}
   cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (Pixmap. file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (.dispose pixmap)
                                                     cursor)))))
