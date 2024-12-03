(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn exit-app []
  (.exit Gdx/app))

(defn post-runnable [runnable]
  (.postRunnable Gdx/app runnable))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn input-x []
  (.getX Gdx/input))

(defn input-y []
  (.getY Gdx/input))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input b))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input k))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input k))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))
