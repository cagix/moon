(ns clojure.core-ext)

(defn clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))
