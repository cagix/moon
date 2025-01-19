(ns cdq.render.tiled-map
  (:require cdq.graphics.tiled-map-renderer
            cdq.graphics.camera
            cdq.graphics.tiled-map))

(defn render [{:keys [cdq.graphics/world-viewport
                      cdq.context/tiled-map
                      cdq.context/raycaster
                      cdq.context/explored-tile-corners]
               :as context}]
  (cdq.graphics.tiled-map-renderer/draw context
                                        tiled-map
                                        (cdq.graphics.tiled-map/tile-color-setter raycaster
                                                                                  explored-tile-corners
                                                                                  (cdq.graphics.camera/position (:camera world-viewport))))
  context)
