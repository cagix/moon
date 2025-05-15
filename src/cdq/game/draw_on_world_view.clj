(ns cdq.game.draw-on-world-view
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)))

(defn do! [draw-fns]
  (Batch/.setColor ctx/batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (Batch/.setProjectionMatrix ctx/batch (camera/combined (:camera (:world-viewport ctx/graphics))))
  (Batch/.begin ctx/batch)
  (graphics/with-line-width ctx/graphics ctx/world-unit-scale
    (fn []
      ; could pass new 'g' with assoc :unit-scale -> but using ctx/graphics accidentally
      ; -> icon is drawn at too big ! => mutable field.
      (reset! (:unit-scale ctx/graphics) ctx/world-unit-scale)
      (utils/execute! draw-fns)
      (reset! (:unit-scale ctx/graphics) 1)))
  (Batch/.end ctx/batch))
