(ns clojure.decl)

(defn assoc* [ctx k [f & params]]
  (assoc ctx k (apply f params)))
