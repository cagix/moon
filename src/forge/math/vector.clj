(ns forge.math.vector
  (:require [forge.math.utils :refer [equal?]])
  (:import (com.badlogic.gdx.math Vector2)))

(defn- m-v2
  ([[x y]] (Vector2. x y))
  ([x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x ^Vector2 v) (.y ^Vector2 v)])

(defn scale [v n]
  (->p (.scl ^Vector2 (m-v2 v) (float n))))

(defn normalise [v]
  (->p (.nor ^Vector2 (m-v2 v))))

(defn add [v1 v2]
  (->p (.add ^Vector2 (m-v2 v1)
             ^Vector2 (m-v2 v2))))

(defn length [v]
  (.len ^Vector2 (m-v2 v)))

(defn distance [v1 v2]
  (.dst ^Vector2 (m-v2 v1) ^Vector2 (m-v2 v2)))

(defn normalised? [v]
  (equal? 1 (length v)))

(defn normal-vectors [[x y]]
  [[(- (float y))         x]
   [          y (- (float x))]])

(defn direction [[sx sy] [tx ty]]
  (normalise [(- (float tx) (float sx))
              (- (float ty) (float sy))]))

(defn angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"
  [v]
  (.angleDeg (m-v2 v) (Vector2. 0 1)))

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
     (.angleDeg (m-v2 v) (Vector2. 0 1))
     (get-angle-from-vector (m-v2 v))]))

 )

(defn diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))
