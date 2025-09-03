(ns cdq.math)

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

(def float-rounding-error (double 0.000001)) ; <- FIXME clojure uses doubles?

(defn nearly-equal? [^double x ^double y]
  (<= (Math/abs (- x y)) float-rounding-error))
