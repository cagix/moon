(ns cdq.graphics.create.world-viewport
  (:require [clojure.gdx.graphics.orthographic-camera :as orthographic-camera])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create
  [{:keys [graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (FitViewport. world-width
                                                           world-height
                                                           (orthographic-camera/create
                                                            :y-down? false
                                                            :world-width world-width
                                                            :world-height world-height)))))
