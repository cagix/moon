(ns cdq.entity)

(defn position [{:keys [entity/body]}]
  (:body/position body))
