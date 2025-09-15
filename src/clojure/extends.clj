(ns clojure.extends
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]))

(defn ns-impls [ctx impls]
  (doseq [[atype-sym implementation-ns-sym protocol-var] (-> impls
                                                             io/resource
                                                             slurp
                                                             edn/read-string
                                                             symbol/require-resolve-symbols)]
    (try (let [atype (eval atype-sym)
               _ (assert (class atype))
               protocol @protocol-var
               method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns-sym "/" name)))))]
           (extend atype protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype-sym atype-sym
                            :implementation-ns-sym implementation-ns-sym
                            :protocol-var protocol-var}
                           t)))))
  ctx)
