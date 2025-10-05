(ns clojure.core-ext)

(defn extend-by-ns [impls]
  (doseq [[atype-sym implementation-ns-sym protocol-sym] impls]
    (try (let [atype (eval atype-sym)
               _ (assert (class atype))
               protocol-var (requiring-resolve protocol-sym)
               protocol @protocol-var
               method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns-sym "/" name)))))]
           (extend atype protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype-sym atype-sym
                            :implementation-ns-sym implementation-ns-sym
                            :protocol-sym protocol-sym}
                           t))))))
