(ns forge.roots.cursor
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn create [[file [hotspot-x hotspot-y]]]
  (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))
