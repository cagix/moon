(ns cdq.entity)

(defn position [{:keys [entity/body]}]
  (:body/position body))

(defprotocol Entity
  (create [_ ctx])
  (create! [_ eid ctx])
  (tick [_ eid ctx]))
