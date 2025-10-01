(ns cdq.graphics.world-viewport
  (:require [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.orthographic-camera :as orthographic-camera]))

(defn create
  [{:keys [graphics/core
           graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (graphics/fit-viewport core
                                                                    world-width
                                                                    world-height
                                                                    (orthographic-camera/create
                                                                     :y-down? false
                                                                     :world-width world-width
                                                                     :world-height world-height)))))
