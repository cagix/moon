(ns cdq.fn-map)

(defn create [{:keys [ks sym-format]}]
  (into {}
        (for [k ks
              :let [sym (symbol (format sym-format (name k)))
                    f (requiring-resolve sym)]]
          (do
           (assert f (str "Cannot resolve " sym))
           [k f]))))
