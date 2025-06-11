(ns clojure.app
  (:require clojure.edn
            clojure.java.io
            clojure.walk))

(defn execute! [[f params]]
  (println "EXECUTE! " [f params])
  (if params
    (f params)
    (f)))

(defn -main [path]
  (->> path
       clojure.java.io/resource
       slurp
       clojure.edn/read-string
       (clojure.walk/postwalk (fn [form]
                                (if (symbol? form)
                                  (if (namespace form) ; var
                                    (requiring-resolve form)
                                    (do
                                     (require form) ; namespace
                                     form))
                                  form)))
       (run! execute!)))
