(ns cdq.ctx.update-mouse
  (:require [cdq.input :as input]
            [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)]
    (update ctx :ctx/graphics #(-> %
                                   (graphics/unproject-ui    mouse-position)
                                   (graphics/unproject-world mouse-position)))))
