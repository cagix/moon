(ns cdq.game.resize
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height))
