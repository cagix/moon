(ns cdq.graphics.tiled-map
  (:require clojure.graphics.tiled-map-renderer
            clojure.graphics.camera
            clojure.graphics.tiled-map))

(defn draw [{:keys [clojure.graphics/world-viewport
                    clojure.context/tiled-map
                    clojure.context/raycaster
                    clojure.context/explored-tile-corners]
             :as context}]
  (clojure.graphics.tiled-map-renderer/draw context
                                            tiled-map
                                            (clojure.graphics.tiled-map/tile-color-setter raycaster
                                                                                          explored-tile-corners
                                                                                          (clojure.graphics.camera/position (:camera world-viewport))))
  context)
