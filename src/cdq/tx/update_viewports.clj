(ns cdq.tx.update-viewports
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics] :as ctx} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height)
  ctx)
