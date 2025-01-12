(ns clojure.graphics)

(defn sprite-batch [_context _config]
  (com.badlogic.gdx.graphics.g2d.SpriteBatch.))

(defn new-cursor [pixmap hotspot-x hotspot-y]
  (.newCursor com.badlogic.gdx.Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor com.badlogic.gdx.Gdx/graphics cursor))

(defn delta-time []
  (.getDeltaTime com.badlogic.gdx.Gdx/graphics))

(defn frames-per-second []
  (.getFramesPerSecond com.badlogic.gdx.Gdx/graphics))

(defn def-color [name-str color]
  (com.badlogic.gdx.graphics.Colors/put name-str color))
