(ns gdl.math)

; available in clojure/math

(def degrees-to-radians (float (/ Math/PI 180)))
(def radians-to-degrees (float (/ 180 Math/PI)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

; available in java latest math

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))
