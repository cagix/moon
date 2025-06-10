(ns gdl.math.utils)

(def degrees-to-radians (float (/ Math/PI 180)))

(def float-rounding-error (float 0.000001))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

(defn nearly-equal? [x y]
  (<= (Math/abs (- x y)) float-rounding-error))
