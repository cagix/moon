(ns cdq.render.clear-screen
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics]
            :as ctx}]
  (graphics/clear-screen! graphics)
  ctx)
