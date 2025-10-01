(ns cdq.graphics.cursors
  (:require [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.utils.disposable :as disposable]))

(defn create
  [{:keys [graphics/core]
    :as graphics}
   cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (graphics/pixmap core file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (disposable/dispose! pixmap)
                                                     cursor)))))
