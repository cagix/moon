(ns cdq.var
  (:import (clojure.lang Var)))

(defn bind-roots! [bindings]
  (doseq [[var-sym value] bindings]
    (Var/.bindRoot (requiring-resolve var-sym) @(requiring-resolve value))))
