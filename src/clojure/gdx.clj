(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn internal [path]
  (.internal Gdx/files path))

(defn sound [path]
  (.newSound Gdx/audio (internal path)))

(defn graphics []
  Gdx/graphics)

(defn input []
  Gdx/input)

(defn cursor [path [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (internal path))
        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn set-input-processor! [input-processor]
  (.setInputProcessor Gdx/input input-processor))
