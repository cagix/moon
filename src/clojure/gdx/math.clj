(ns clojure.gdx.math
  (:refer-clojure :exclude [contains?])
  (:import (com.badlogic.gdx.math Circle
                                  Intersector
                                  MathUtils
                                  Rectangle
                                  Vector2)))

(defn equal? [a b]
  (MathUtils/isEqual a b))

(defn clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn degree->radians [degree]
  (* (float degree) MathUtils/degreesToRadians))

(defn circle [[x y] radius]
  (Circle. x y radius))

(defn rectangle [[x y] width height]
  (Rectangle. x y width height))

(defn contains? [^Rectangle rectangle [x y]]
  (.contains rectangle x y))

(defmulti overlaps? (fn [a b] [(class a) (class b)]))

(defmethod overlaps? [Circle Circle]
  [^Circle a ^Circle b]
  (Intersector/overlaps a b))

(defmethod overlaps? [Rectangle Rectangle]
  [^Rectangle a ^Rectangle b]
  (Intersector/overlaps a b))

(defmethod overlaps? [Rectangle Circle]
  [^Rectangle rect ^Circle circle]
  (Intersector/overlaps circle rect))

(defmethod overlaps? [Circle Rectangle]
  [^Circle circle ^Rectangle rect]
  (Intersector/overlaps circle rect))

(defn v2
  ([[x y]]
   (v2 x y))
  ([x y]
   (Vector2. x y)))
