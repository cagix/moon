(ns clojure.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))

(defn- java-class? [s]
  (boolean (re-matches #".*\.[A-Z][A-Za-z0-9_]*" s)))

(defn- require-resolve-symbols [form]
  (if (symbol? form)
    (if (java-class? (str form))
      form
      (if (namespace form)
        (let [var (requiring-resolve form)]
          (assert var form)
          var)
        form))
    form))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))
