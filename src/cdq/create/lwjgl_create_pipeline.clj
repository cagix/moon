(ns cdq.create.lwjgl-create-pipeline)

(defn do!
  [{:keys [ctx/create-pipeline]
    :as ctx}]
  (reduce (fn [ctx f]
            (let [result (f ctx)]
              (if (nil? result)
                ctx
                result)))
          ctx
          (map requiring-resolve create-pipeline)))
