(ns cdq.game.draw-on-world-view
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera])
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
      (doseq [f draw-fns]
        (f))
      (reset! (:unit-scale ctx/graphics) 1)))
  (Batch/.end (:batch ctx/graphics)))
