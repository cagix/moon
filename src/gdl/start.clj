(ns gdl.start
  (:require clojure.edn
            clojure.java.io
            clojure.walk))

(defn execute! [[f params]]
  (f params))

(defn dispatch [[to-eval mapping]]
  (->> (to-eval)
       (get mapping)
       (run! execute!)))

(defn slurpquire [path]
  (->> path
       clojure.java.io/resource
       slurp
       clojure.edn/read-string
       (clojure.walk/postwalk (fn [form]
                                (if (symbol? form)
                                  (if (namespace form)
                                    (requiring-resolve form)
                                    (try (require form)
                                         form
                                         (catch Exception e ; Java classes
                                           form)))
                                  form)))))

(defn -main [path]
  (->> path
       slurpquire
       (run! execute!)))
