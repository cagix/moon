(ns cdq.game.draw-on-world-view
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.graphics :as graphics])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)))

(defn do! [draw-fns]
  (Batch/.setColor (:batch ctx/graphics) Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (Batch/.setProjectionMatrix (:batch ctx/graphics) (camera/combined (:camera (:world-viewport ctx/graphics))))
  (Batch/.begin (:batch ctx/graphics))
  (graphics/with-line-width ctx/graphics (:world-unit-scale ctx/graphics)
    (fn []
      ; could pass new 'g' with assoc :unit-scale -> but using ctx/graphics accidentally
      ; -> icon is drawn at too big ! => mutable field.
      (reset! (:unit-scale ctx/graphics) (:world-unit-scale ctx/graphics))
      (utils/execute! draw-fns)
      (reset! (:unit-scale ctx/graphics) 1)))
  (Batch/.end (:batch ctx/graphics)))
