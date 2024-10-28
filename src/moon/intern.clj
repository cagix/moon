(ns moon.intern)

(defn into-clojure.core [nmspace]
  (in-ns 'clojure.core)
  (require nmspace)
  (require 'potemkin.namespaces)
  (let [syms (map #(symbol (str nmspace) (name %))
                  (keys (ns-publics nmspace)))]
    (eval `(potemkin.namespaces/import-vars ~@syms))))
