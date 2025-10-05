(ns cdq.graphics.create.world-viewport
  (:require [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]))

(defn create
  [{:keys [graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (fit-viewport/create world-width
                                                                  world-height
                                                                  (orthographic-camera/create
                                                                   :y-down? false
                                                                   :world-width world-width
                                                                   :world-height world-height)))))
