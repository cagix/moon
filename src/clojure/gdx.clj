(ns clojure.gdx
  (:require [clojure.files]
            [clojure.graphics])
  (:import (com.badlogic.gdx Gdx)))

(defn files [_context]
  (let [this Gdx/files]
    (reify clojure.files/Files
      (internal [_ path]
        (.internal this path)))))

(defn graphics [_context]
  (let [this Gdx/graphics]
    (reify clojure.graphics/Graphics
      (new-cursor [_ pixmap hotspot-x hotspot-y]
        (.newCursor this pixmap hotspot-x hotspot-y))

      (set-cursor [_ cursor]
        (.setCursor this cursor))

      (delta-time [_]
        (.getDeltaTime this))

      (frames-per-second [_]
        (.getFramesPerSecond this)))))
