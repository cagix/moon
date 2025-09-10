(ns cdq.ui.dev-menu.update-labels.fps
  (:require [cdq.gdx.graphics :as graphics]))

(defn create [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})
