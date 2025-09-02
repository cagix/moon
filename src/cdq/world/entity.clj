(ns cdq.world.entity)

(defn position [{:keys [entity/body]}]
  (:body/position body))
