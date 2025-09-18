(ns clojure.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))

(defn require-resolve-symbols [form]
  (if (and (symbol? form)
           (namespace form))
    (let [var (requiring-resolve form)]
      (assert var form)
      var)
    form))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))
