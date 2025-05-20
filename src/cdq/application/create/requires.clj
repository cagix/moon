(ns cdq.application.create.requires)

(defn do! [{:keys [ctx/config]}]
  (run! require (:requires config)))
