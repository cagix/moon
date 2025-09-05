(ns cdq.var
  (:require [clojure.walk :as walk])
  (:import (clojure.lang Var)))

(defn bind-roots! [bindings]
  (doseq [[var-sym value] bindings]
    (Var/.bindRoot (requiring-resolve var-sym)
                   (walk/postwalk
                    (fn [x]
                      (if (and (symbol? x) (namespace x))
                        (try
                         @(requiring-resolve x)
                         (catch Exception _ x))
                        x))
                    value))))
