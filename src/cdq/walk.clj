(ns cdq.walk
  (:require [clojure.walk :as walk]))

(defn require-resolve-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (let [var (requiring-resolve form)]
                       (assert var form)
                       var)
                     form))
                 form))
