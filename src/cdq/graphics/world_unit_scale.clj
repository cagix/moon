(ns cdq.graphics.world-unit-scale)

(defn create [{:keys [gdl/config] :as context}]
  (assoc context :gdl.graphics/world-unit-scale (float (/ (::tile-size config)))))
