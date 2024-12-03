(ns forge.roots.gdx
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn cursor [file [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (.internal Gdx/files file))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))
