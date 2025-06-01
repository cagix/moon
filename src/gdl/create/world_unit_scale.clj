(ns gdl.create.world-unit-scale)

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (assoc ctx :ctx/world-unit-scale (float (/ (:tile-size config)))))
