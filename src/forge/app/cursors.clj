(ns ^:no-doc forge.app.cursors
  (:require [clojure.gdx :as gdx]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils Disposable)))

(defmethods :app/cursors
  (app-create [[_ cursors]]
    (bind-root #'cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                                          cursor (gdx/cursor pixmap hotspot-x hotspot-y)]
                                      (.dispose pixmap)
                                      cursor))
                                  cursors)))
  (app-dispose [_]
    (run! Disposable/.dispose (vals cursors))))
