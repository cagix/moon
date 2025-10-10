(ns cdq.game.create.input-processor
  (:require [cdq.input :as input]))

(defn do!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (input/set-processor! input stage)
  ctx)
