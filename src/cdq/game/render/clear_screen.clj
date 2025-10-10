(ns cdq.game.render.clear-screen
  (:require [cdq.graphics :as graphics]
            [clojure.color :as color]))

(defn step
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics color/black)
  ctx)
