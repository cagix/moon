(ns clojure.gdx.math.vector2
  (:import (com.badlogic.gdx.math Vector2)))

(defn ->clj [^Vector2 vector2]
  [(.x vector2)
   (.y vector2)])

(defn ->java [[x y]]
  (Vector2. x y))
