(ns gdl.application)

(defn provide-impl-namespace [impls]
  (doseq [[atype implementation-ns protocol] impls]
    (let [atype (eval atype)
          protocol @protocol
          method-map (update-vals (:sigs protocol)
                                  (fn [{:keys [name]}]
                                    (requiring-resolve (symbol (str implementation-ns "/" name)))))]
      (extend atype protocol method-map))))
