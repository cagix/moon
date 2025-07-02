(ns gdl.start
  (:require clojure.edn
            clojure.java.io
            clojure.walk
            master.yoda)
  (:gen-class))

(defn slurpquire
  ([_context path]
   (slurpquire path))
  ([path]
   (->> path
        clojure.java.io/resource
        slurp
        clojure.edn/read-string
        (clojure.walk/postwalk master.yoda/req))))

(defn -main [path]
  (->> path
       slurpquire
       (run! master.yoda/execute!)))
