(ns cdq.ui.dev-menu.update-labels.fps
  (:require [cdq.gdx.graphics :as graphics]))

(defn create [icon]
  {:label "FPS"
   :update-fn graphics/frames-per-second
   :icon icon})
