(ns cdq.game.draw-on-world-view
  (:require [cdq.draw :as draw]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [gdl.graphics.batch :as batch]))

(defn do! [draw-fns]
  (batch/draw-on-viewport! ctx/batch
                           ctx/world-viewport
                           (fn []
                             (draw/with-line-width ctx/world-unit-scale
                               (fn []
                                 (reset! ctx/unit-scale ctx/world-unit-scale)
                                 (utils/execute! draw-fns)
                                 (reset! ctx/unit-scale 1))))))
