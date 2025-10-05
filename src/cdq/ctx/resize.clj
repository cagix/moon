(ns cdq.ctx.resize
  (:require [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]))

(defn do! [{:keys [ctx/graphics]} width height]
  (ui-viewport/update!    graphics width height)
  (world-viewport/update! graphics width height))
