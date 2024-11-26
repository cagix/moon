(ns clojure.gdx
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)))

(defmacro application [& functions]
  `(proxy [ApplicationAdapter] []
     ~@functions))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn exit-app []
  (.exit Gdx/app))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn new-cursor [pixmap [hotspot-x hotspot-y]]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn mouse-x []
  (.getX Gdx/input))

(defn mouse-y []
  (.getY Gdx/input))

(defn set-input-processor [processor]
  (.setInputProcessor Gdx/input processor))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn button-just-pressed? [button]
  (.isButtonJustPressed Gdx/input button))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input k))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input k))
