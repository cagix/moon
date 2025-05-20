(ns cdq.application.create.world-unit-scale)

(defn do! [{:keys [ctx/config]}]
  (float (/ (:tile-size config))))
