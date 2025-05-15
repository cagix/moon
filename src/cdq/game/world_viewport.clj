(ns cdq.game.world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]))

(defn do! []
  (utils/bind-root #'ctx/world-viewport
                   (graphics/world-viewport ctx/world-unit-scale
                                            {:width 1440
                                             :height 900})))
