(ns gdl.math)

(def degrees-to-radians (float (/ Math/PI 180)))
(def radians-to-degrees (float (/ 180 Math/PI)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))
