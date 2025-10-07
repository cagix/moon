(ns cdq.ctx.render.clear-screen
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)
