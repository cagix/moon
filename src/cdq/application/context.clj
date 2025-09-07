(ns cdq.application.context)

(defn create [ctx create-pipeline]
  (reduce (fn [ctx f]
            (let [result (if (vector? f)
                           (let [[f params] f]
                             ((requiring-resolve f) ctx params))
                           ((requiring-resolve f) ctx))]
              (if (nil? result)
                ctx
                result)))
          ctx
          create-pipeline))

(defn render [ctx pipeline]
  (reduce (fn [ctx f]
            (if-let [new-ctx ((requiring-resolve f) ctx)]
              new-ctx
              ctx))
          ctx
          pipeline))
