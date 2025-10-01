(ns cdq.graphics.tiled-map
  (:require [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]))

(defn renderer [{:keys [graphics/batch
                        graphics/world-unit-scale]
                 :as graphics}]
  (assoc graphics :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))
