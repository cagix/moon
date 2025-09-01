(ns cdq.start.bind-root
  (:require [clojure.walk :as walk]))

(defn do! [[var-sym value]]
  (.bindRoot (requiring-resolve var-sym)
             (walk/postwalk
              (fn [x]
                (if (and (symbol? x) (namespace x))
                  (try
                   (requiring-resolve x) ;; requires ns if not loaded, returns var
                   (catch Exception _ x)) ;; leave symbol if cannot resolve
                  x))
              value)))
