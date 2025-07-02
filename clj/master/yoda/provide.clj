(ns master.yoda.provide)

(defn do! [impls]
  (doseq [[atype implementation-ns protocol] impls]
    (try (let [protocol @protocol
               method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns "/" name)))))]
           (extend atype protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype atype
                            :implementation-ns implementation-ns
                            :protocol protocol}
                           t))))))
