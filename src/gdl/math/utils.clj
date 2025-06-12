(ns gdl.math.utils)

(def float-rounding-error (float 0.000001)) ; <- but clojure uses doubles?

(defn nearly-equal? [x y]
  (<= (Math/abs (- x y)) float-rounding-error))
