(ns cdq.ui.dev-menu.update-labels.fps
  (:require [clojure.gdx.graphics :as graphics]))

(defn create [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})
