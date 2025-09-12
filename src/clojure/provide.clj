(ns clojure.provide)

(defn do! [impls]
  (doseq [[atype implementation-ns-str protocol] impls]
    (try (let [method-map (update-vals (:sigs protocol)
                                       (fn [{:keys [name]}]
                                         (requiring-resolve (symbol (str implementation-ns-str "/" name)))))]
           (clojure.pprint/pprint method-map)
           (extend atype protocol method-map))
         (catch Throwable t
           (throw (ex-info "Cant extend"
                           {:atype atype
                            :implementation-ns implementation-ns-str
                            :protocol protocol}
                           t))))))
