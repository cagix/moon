(ns cdq.render.check-open-debug-data
  (:require [cdq.input :as input]))

(defn do!
  [{:keys [ctx/input]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    ((requiring-resolve 'cdq.game.open-debug-data-window/do!) ctx))
  ctx)
