(ns clojure.core-ext)

(defn call [[f & params]]
  (apply f params))
