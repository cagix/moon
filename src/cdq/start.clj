(ns cdq.start
  (:require clojure.edn
            clojure.java.io
            clojure.walk
            cdq.core)
  (:gen-class))

(defn slurpquire
  ([_context path] ; unused
   (slurpquire path))
  ([path]
   (->> path
        clojure.java.io/resource
        slurp
        clojure.edn/read-string
        (clojure.walk/postwalk cdq.core/req))))

(defn -main [path]
  (->> path
       slurpquire
       (run! cdq.core/execute!)))
