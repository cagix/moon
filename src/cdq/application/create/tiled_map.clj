(ns cdq.application.create.tiled-map)

(defn create [context]
  (:tiled-map (:cdq.context/level context)))
