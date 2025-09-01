(ns cdq.math)

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))
