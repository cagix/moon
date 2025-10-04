(ns cdq.ctx.render.update-mouse
  (:require [cdq.input :as input]
            [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (update ctx :ctx/graphics graphics/unproject-world (input/mouse-position input)))
