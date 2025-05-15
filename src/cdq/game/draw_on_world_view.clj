(ns cdq.game.draw-on-world-view
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)))

(defn do! [draw-fns]
  (Batch/.setColor ctx/batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (Batch/.setProjectionMatrix ctx/batch (camera/combined (:camera ctx/world-viewport)))
  (Batch/.begin ctx/batch)
  (graphics/with-line-width ctx/world-unit-scale
    (fn []
      (reset! ctx/unit-scale ctx/world-unit-scale)
      (utils/execute! draw-fns)
      (reset! ctx/unit-scale 1)))
  (Batch/.end ctx/batch))
