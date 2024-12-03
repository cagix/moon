(ns forge.app.cursors
  (:require [clojure.gdx :as gdx]
            [forge.system :as system :refer [defmethods bind-root mapvals]])
  (:import (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils Disposable)))

(defmethods :app/cursors
  (system/create [[_ cursors]]
    (bind-root #'system/cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                            (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                                                  cursor (gdx/cursor pixmap hotspot-x hotspot-y)]
                                              (.dispose pixmap)
                                              cursor))
                                          cursors)))
  (system/dispose [_]
    (run! Disposable/.dispose (vals system/cursors))))
