(ns clojure.graphics)

(defn new-cursor [pixmap hotspot-x hotspot-y]
  (.newCursor com.badlogic.gdx.Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor com.badlogic.gdx.Gdx/graphics cursor))
