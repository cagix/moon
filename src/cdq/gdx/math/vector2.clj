(ns cdq.gdx.math.vector2
  (:require [cdq.utils :as utils])
  (:import (com.badlogic.gdx.math Vector2)))

(defn- m-v2
  (^Vector2 [[x y]] (Vector2. x y))
  (^Vector2 [x y]   (Vector2. x y)))

(defn- ->p [^Vector2 v]
  [(.x v) (.y v)])

(defn nearly-equal? [[x1 y1] [x2 y2]]
  (and (utils/nearly-equal? x1 x2)
       (utils/nearly-equal? y1 y2)))

(defn scale [[^double x ^double y] ^double scalar]
  [(* x scalar)
   (* y scalar)])

(defn length [[^double x ^double y]]
  (Math/sqrt (+ (* x x)
                (* y y))))

(defn normalise [v]
;	public Vector2 nor () {
;		float len = len();
;		if (len != 0) {
;			x /= len;
;			y /= len;
;		}
;		return this;
;	}
  (->p (.nor (m-v2 v))))

(defn add [v1 v2]
;	public Vector2 add (Vector2 v) {
;		x += v.x;
;		y += v.y;
;		return this;
;	}
  (->p (.add (m-v2 v1) (m-v2 v2))))

(defn distance [v1 v2]
;	public static float dst (float x1, float y1, float x2, float y2) {
;		final float x_d = x2 - x1;
;		final float y_d = y2 - y1;
;		return (float)Math.sqrt(x_d * x_d + y_d * y_d);
;	}
  (.dst (m-v2 v1) (m-v2 v2)))

(defn nearly-normalised? [v]
  (utils/nearly-equal? 1 (length v)))

(defn direction [[sx sy] [tx ty]]
  (normalise [(- (float tx) (float sx))
              (- (float ty) (float sy))]))

(defn angle-from-vector
  "converts theta of Vector2 to angle from top (top is 0 degree, moving left is 90 degree etc.), counterclockwise"
  [v]

;	/** @return the angle in degrees of this vector (point) relative to the given vector. Angles are towards the positive y-axis
;	 *         (typically counter-clockwise.) in the  0, 360  range */
;	public float angleDeg (Vector2 reference) {
;		float angle = (float)Math.atan2(reference.crs(this), reference.dot(this)) * MathUtils.radiansToDegrees;
;		if (angle < 0) angle += 360;
;		return angle;
;	}
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

(defn normal-vectors [[x y]]
  [[(- (float y))         x]
   [          y (- (float x))]])

(defn diagonal-direction? [[x y]]
  (and (not (zero? (float x)))
       (not (zero? (float y)))))

(defn add-vs [vs]
  (normalise (reduce add [0 0] vs)))
