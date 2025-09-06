(ns cdq.create.tiled-map-renderer
  (:require [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]))

(defn do!
  [{:keys [ctx/world-unit-scale
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))
