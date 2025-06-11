(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn internal [path]
  (.internal Gdx/files path))

(defn sound [path]
  (.newSound Gdx/audio (internal path)))

(defn graphics []
  Gdx/graphics)

(defn input []
  Gdx/input)
