(ns forge.app.cursors
  (:require [clojure.gdx :as gdx]
            [forge.context :as context]
            [forge.lifecycle :as lifecycle]
            [forge.system :refer [defmethods bind-root mapvals]])
  (:import (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils Disposable)))

(defmethods :app/cursors
  (lifecycle/create [[_ cursors]]
    (bind-root #'context/cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                            (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                                                  cursor (gdx/cursor pixmap hotspot-x hotspot-y)]
                                              (.dispose pixmap)
                                              cursor))
                                          cursors)))
  (lifecycle/dispose [_]
    (run! Disposable/.dispose (vals context/cursors))))
