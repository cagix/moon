(ns ^:no-doc forge.app.cursors
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils Disposable)))

(defmethods :app/cursors
  (app-create [[_ cursors]]
    (bind-root #'cursors (mapvals (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                          cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                      (.dispose pixmap)
                                      cursor))
                                  cursors)))
  (app-dispose [_]
    (run! Disposable/.dispose (vals cursors))))
