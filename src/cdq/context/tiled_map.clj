(ns cdq.context.tiled-map)

(defn create [_ {:keys [cdq.context/level]}]
  (:tiled-map level))
