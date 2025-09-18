(ns cdq.game.resize
  (:require [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics] :as ctx} width height]
  (graphics/update-viewports! graphics width height)
  ctx)
