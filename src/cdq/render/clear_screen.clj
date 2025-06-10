(ns cdq.render.clear-screen
  (:require [gdl.graphics :as g]))

(defn do! [{:keys [ctx/graphics]}]
  (g/clear-screen! graphics :black)
  ctx)
