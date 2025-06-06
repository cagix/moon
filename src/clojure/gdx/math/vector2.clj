(ns clojure.gdx.math.vector2
  (:import (com.badlogic.gdx.math Vector2)))

(defn create [x y]
  (Vector2. x y))

(defn ->clj-vec [^Vector2 v]
  [(.x v) (.y v)])
