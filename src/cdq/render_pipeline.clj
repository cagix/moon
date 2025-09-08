(ns cdq.render-pipeline)

(defn do!
  [{:keys [ctx/render-pipeline]
    :as ctx}]
  (reduce (fn [ctx f]
            (if-let [new-ctx (f ctx)]
              new-ctx
              ctx))
          ctx
          (map requiring-resolve render-pipeline)))
