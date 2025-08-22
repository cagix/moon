(ns gdl.math.utils)

(def float-rounding-error (double 0.000001)) ; <- but clojure uses doubles?

(defn nearly-equal? [^double x ^double y]
  (<= (Math/abs (- x y)) float-rounding-error))
