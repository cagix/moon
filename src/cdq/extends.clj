(ns cdq.extends)

(defn impls! [impls]
  (doseq [[atype implementation-ns protocol-sym] impls]
    (try (let [protocol @(requiring-resolve protocol-sym)
               method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns "/" name)))))]
           (extend (eval atype) protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype atype
                            :implementation-ns implementation-ns
                            :protocol protocol-sym}
                           t))))))
