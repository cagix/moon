(ns cdq.create.requires)

(defn create [{:keys [ctx/config]}]
  (run! require (:requires config)))
