(ns cdq.world-fns.modules.calculate-start-position)

(defn do! [{:keys [start scale] :as w}]
  (assoc w :start-position (mapv * start scale)))
