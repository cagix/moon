(ns cdq.ctx.create)

(defn do! [{:keys [initial-value
                   create-pipeline]}]
  (fn []
    (reduce (fn [ctx f]
              (let [result (if (vector? f)
                             (let [[f params] f]
                               ((requiring-resolve f) ctx params))
                             ((requiring-resolve f) ctx))]
                (if (nil? result)
                  ctx
                  result)))
            initial-value
            create-pipeline)))
