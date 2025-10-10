(ns cdq.game.create.input-processor
  (:require [clojure.input :as input]))

(defn do!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (input/set-processor! input stage)
  ctx)
