(ns clojure.gdx
  (:require [clojure.gdx.app :as app]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics])
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

      (set-cursor! [_ cursor]
        (.setCursor this cursor)))))

(defn input []
  Gdx/input)
