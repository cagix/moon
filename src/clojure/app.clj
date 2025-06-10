(ns clojure.app
  (:require clojure.edn
            clojure.java.io
            clojure.walk))

(defn execute! [[f params]]
  (f params))

(defn -main [path]
  (->> path
       clojure.java.io/resource
       slurp
       clojure.edn/read-string
       (clojure.walk/postwalk (fn [form]
                                (if (symbol? form)
                                  (if (namespace form)
                                    (requiring-resolve form)
                                    (do
                                     (require form)
                                     form))
                                  form)))
       (run! execute!)))

