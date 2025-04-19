(ns clojure.gdx.graphics
  (:import (com.badlogic.gdx Gdx)))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))
