(ns cdq.game.resize
  (:require [cdq.ctx.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))
