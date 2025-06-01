(ns clojure.gdx
  (:require [gdl.app :as app]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.input :as input])
  (:import (com.badlogic.gdx Gdx)))

(defn app []
  (let [this Gdx/app]
    (reify app/App
      (post-runnable! [_ runnable]
        (.postRunnable this runnable)))))

(defn files []
  (let [this Gdx/files]
    (reify files/Files
      (internal [_ path]
        (.internal this path)))))

(defn graphics []
  (let [this Gdx/graphics]
    (reify graphics/Graphics
      (delta-time [_]
        (.getDeltaTime this))

      (frames-per-second [_]
        (.getFramesPerSecond this))

      (new-cursor [_ pixmap hotspot-x hotspot-y]
        (.newCursor this pixmap hotspot-x hotspot-y))

      (set-cursor! [_ cursor]
        (.setCursor this cursor)))))
