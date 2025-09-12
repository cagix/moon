(ns clojure.start
  (:require cdq.effect
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.object :as object]
            [clojure.walk :as walk])
  (:gen-class))

(defn return-identity [_ v _db]
  v)

(defn require-resolve-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (let [var (requiring-resolve form)]
                       (assert var form)
                       var)
                     form))
                 form))

(defn -main []
  (.bindRoot #'cdq.effect/k->method-map (require-resolve-symbols
                                         (-> "effects_fn_map.edn"
                                             io/resource
                                             slurp
                                             edn/read-string)))
  (-> "clojure.start.edn"
      io/resource
      slurp
      edn/read-string
      require-resolve-symbols
      object/pipeline))
