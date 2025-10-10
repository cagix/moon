(ns cdq.game.render.update-mouse
  (:require [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [cdq.input :as input]))

(defn step
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (world-viewport/unproject graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position] (ui-viewport/unproject graphics mp)))))
