(ns cdq.math)

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn degree->radians [degree]
  (* degrees-to-radians (float degree)))
