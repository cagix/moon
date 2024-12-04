(ns ^:no-doc forge.app.cursors
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defmethods :app/cursors
  (app-create [[_ data]]
    (bind-root #'cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                          cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                      (dispose pixmap)
                                      cursor))
                                  data)))
  (app-dispose [_]
    (run! dispose (vals cursors))))
