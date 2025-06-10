(ns gdl.create.config
  (:require clojure.edn
            clojure.java.io
            clojure.walk)
  (:import (clojure.lang ILookup)))

(defn do! [_ctx config-path]
  (let [m (->> config-path
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
                                          form))))]
    (reify ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))))
