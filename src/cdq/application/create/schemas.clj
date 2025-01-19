(ns cdq.application.create.schemas
  (:require clojure.edn
            clojure.java.io))

(defn create [_context]
  (-> "schema.edn"
      clojure.java.io/resource
      slurp
      clojure.edn/read-string))
