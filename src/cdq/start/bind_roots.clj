(ns cdq.start.bind-roots)

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (doseq [[var-sym to-set-sym] (:cdq.start.bind-roots config)]
    (try (.bindRoot (requiring-resolve var-sym) @(requiring-resolve to-set-sym))
         (catch Throwable t
           (throw (ex-info ""
                           {:var-sym var-sym
                            :to-set-sym to-set-sym}
                           t)))))
  ctx)
