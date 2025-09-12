(ns cdq.render.clear-screen
  (:require [cdq.ctx.graphics :as graphics]
            [clojure.graphics.color :as color]))

(defn do!
  [{:keys [ctx/graphics]}]
  (graphics/clear! graphics color/black))
