(ns cdq.ctx.update-viewports
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]} width height]
  (graphics/update-viewports! graphics width height))
