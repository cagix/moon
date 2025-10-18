(ns clojure.core-ext)

(defn call [[f & params]]
  (apply f params))

(defn pipeline [ctx steps]
  (reduce (fn [ctx f]
            (f ctx))
          ctx
          steps))
