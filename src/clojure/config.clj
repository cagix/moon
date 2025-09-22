(ns clojure.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk])
  (:gen-class))

(defn require-resolve-symbols [form]
  (if (and (symbol? form)
           (namespace form))
    (let [avar (requiring-resolve form)]
      (assert avar form)
      avar)
    form))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))

(defn execute!
  [[f & params]]
  (apply f params))

(defn -main [path]
  (->> path
       edn-resource
       (run! execute!)))
