(ns cdq.game.world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.impl.graphics :as graphics]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn- world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (graphics/fit-viewport world-width world-height camera)))

(defn do! []
  (utils/bind-root #'ctx/world-viewport (world-viewport ctx/world-unit-scale
                                                        {:width 1440
                                                         :height 900})))
