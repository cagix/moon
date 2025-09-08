(ns cdq.start.bind-roots)

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (doseq [[var-sym to-set-sym] (:cdq.start.bind-roots config)]
    (.bindRoot (requiring-resolve var-sym) @(requiring-resolve to-set-sym)))
  ctx)
