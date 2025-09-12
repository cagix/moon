(ns cdq.ui.dev-menu.update-labels.fps
  (:require [cdq.ctx.graphics :as graphics]))

(defn create [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})
