(ns clojure.gdx
  (:require [clojure.files]
            [clojure.graphics]
            [clojure.input]
            [clojure.interop :refer [k->input-button k->input-key]])
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

(defn input [_context]
  (let [this Gdx/input]
    (reify clojure.input/Input
      (x [_]
        (.getX this))

      (y [_]
        (.getY this))

      (button-just-pressed? [_ button]
        (.isButtonJustPressed this (k->input-button button)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed this (k->input-key key)))

      (key-pressed? [_ key]
        (.isKeyPressed this (k->input-key key)))

      (set-processor [_ input-processor]
        (.setInputProcessor this input-processor)))))
