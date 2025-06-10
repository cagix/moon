(ns clojure.gdx.math.utils)

(def degrees-to-radians (float (/ Math/PI 180)))

(def float-rounding-error (float 0.000001)) ; 32 bits

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

(defn nearly-equal?
  "Returns true if a is nearly equal to b. The function uses the default floating error tolerance."
  [x y]
  (<= (Math/abs (- x y)) float-rounding-error))
