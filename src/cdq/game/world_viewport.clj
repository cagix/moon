(ns cdq.game.world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! []
  (utils/bind-root #'ctx/world-viewport
                   (graphics/world-viewport ctx/world-unit-scale
                                            ctx/world-viewport-config)))
