(ns forge.app.world-viewport
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.viewport :as vp :refer [fit-viewport]]
            [forge.core :refer [bind-root
                                world-unit-scale
                                world-viewport-width
                                world-viewport-height
                                world-viewport]]))

(defn create [[_ [width height tile-size]]]
  (bind-root world-unit-scale (float (/ tile-size)))
  (bind-root world-viewport-width  width)
  (bind-root world-viewport-height height)
  (bind-root world-viewport (let [world-width  (* width  world-unit-scale)
                                  world-height (* height world-unit-scale)
                                  camera (g/orthographic-camera)
                                  y-down? false]
                              (.setToOrtho camera y-down? world-width world-height)
                              (fit-viewport world-width world-height camera))))
(defn resize [_ w h]
  (vp/update world-viewport w h))
