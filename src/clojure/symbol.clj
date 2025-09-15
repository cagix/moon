(ns clojure.symbol
  (:require [clojure.walk :as walk]))

(defn java-class? [s]
  (boolean (re-matches #".*\.[A-Z][A-Za-z0-9_]*" s)))

(defn require-resolve-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (if (java-class? (str form))
                       form
                       (if (namespace form)
                         (let [var (requiring-resolve form)]
                           (assert var form)
                           var)
                         form))
                     form))
                 form))
