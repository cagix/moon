(ns clojure.gdx
  (:require [clojure.gdx.interop :as interop])
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn internal [path]
  (.internal Gdx/files path))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor! [cursor]
  (.setCursor Gdx/graphics cursor))

(defn input-x []
  (.getX Gdx/input))

(defn input-y []
  (.getY Gdx/input))

(defn set-input-processor! [input-processor]
  (.setInputProcessor Gdx/input input-processor))

(defn button-just-pressed? [button]
  (.isButtonJustPressed Gdx/input (interop/k->input-button button)))

(defn key-just-pressed? [key]
  (.isKeyJustPressed Gdx/input (interop/k->input-key key)))

(defn key-pressed? [key]
  (.isKeyPressed Gdx/input (interop/k->input-key key)))
