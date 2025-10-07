(ns cdq.graphics.create.tiled-map-renderer
  (:require [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]))

(defn create
  [{:keys [graphics/batch
           graphics/world-unit-scale]
    :as graphics}]
  (assoc graphics :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))
