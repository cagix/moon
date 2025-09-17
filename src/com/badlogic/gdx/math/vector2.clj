(ns com.badlogic.gdx.math.vector2
  (:import (com.badlogic.gdx.math Vector2)))

(defn ->clj [^Vector2 vector2]
  [(.x vector2)
   (.y vector2)])

(defn ->java
  ([[x y]]
   (Vector2. x y))
  ([x y]
   (->java x y)))

(defn scale [[^double x ^double y] ^double scalar]
  [(* x scalar)
   (* y scalar)])

(defn length [[^double x ^double y]]
  (Math/sqrt (+ (* x x)
                (* y y))))

(defn normalise [v]
  (->clj (.nor (->java v))))

(defn add [v1 v2]
  (->clj (.add (->java v1) (->java v2))))

(defn distance [v1 v2]
  (.dst (->java v1) (->java v2)))

(defn direction [[sx sy] [tx ty]]
  (normalise [(- (float tx) (float sx))
              (- (float ty) (float sy))]))

(defn angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"
  [v]
  (.angleDeg (->java v) (Vector2. 0 1)))

(comment

 (pprint
  (for [v [[0 1]
           [1 1]
           [1 0]
           [1 -1]
           [0 -1]
           [-1 -1]
           [-1 0]
           [-1 1]]]
    [v
     (.angleDeg (->java v) (Vector2. 0 1))
     (get-angle-from-vector (->java v))]))

 )

(defn normal-vectors [[x y]]
  [[(- (float y))         x]
   [          y (- (float x))]])

(defn diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))

(defn add-vs [vs]
  (normalise (reduce add [0 0] vs)))
