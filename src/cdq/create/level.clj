(ns cdq.create.level)

(defn create [ctx world-fn]
  ((requiring-resolve world-fn) ctx))

(defn tiled-map [{:keys [ctx/level]}]
  (:tiled-map level))

(defn start-position [{:keys [ctx/level]}]
  (:start-position level))
