(ns cdq.game.render.update-mouse
  (:require [cdq.input :as input]
            [cdq.graphics :as graphics]))

(defn step
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (graphics/unproject-world graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position] (graphics/unproject-ui graphics mp)))))
