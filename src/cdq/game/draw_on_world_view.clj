(ns cdq.game.draw-on-world-view
  (:require [cdq.batch :as batch]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.graphics :as graphics]))

(defn do! [draw-fns]
  (batch/draw-on-viewport! ctx/batch
                           ctx/world-viewport
                           (fn []
                             (graphics/with-line-width ctx/world-unit-scale
                               (fn []
                                 (reset! ctx/unit-scale ctx/world-unit-scale)
                                 (utils/execute! draw-fns)
                                 (reset! ctx/unit-scale 1))))))
