(ns cdq.game.resize
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))
