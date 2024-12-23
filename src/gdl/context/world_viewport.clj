(ns gdl.context.world-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]
            [gdl.context :as ctx]))

(defn setup [{:keys [tile-size width height]}]
  (bind-root ctx/world-unit-scale (float (/ tile-size)))
  (bind-root ctx/world-viewport-width  width)
  (bind-root ctx/world-viewport-height height)
  (bind-root ctx/camera (camera/orthographic))
  (bind-root ctx/world-viewport (let [world-width  (* width  ctx/world-unit-scale)
                                      world-height (* height ctx/world-unit-scale)]
                                  (camera/set-to-ortho ctx/camera
                                                       world-width
                                                       world-height
                                                       :y-down? false)
                                  (viewport/fit world-width
                                                world-height
                                                ctx/camera))))
