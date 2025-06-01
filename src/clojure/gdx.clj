(ns clojure.gdx
  (:require [clojure.gdx.app :as app]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.input :as input]
            [clojure.gdx.interop :as interop])
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

      (cursor [_ pixmap hotspot-x hotspot-y]
        (.newCursor this pixmap hotspot-x hotspot-y))

      (set-cursor! [_ cursor]
        (.setCursor this cursor)))))

(defn input []
  (let [this Gdx/input]
    (reify input/Input
      (button-just-pressed? [_ button]
        (.isButtonJustPressed this (interop/k->input-button button)))

      (key-pressed? [_ key]
        (.isKeyPressed this (interop/k->input-key key)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed this (interop/k->input-key key)))

      (set-processor! [_ input-processor]
        (.setInputProcessor this input-processor))

      (mouse-position [_]
        [(.getX this)
         (.getY this)]))))
