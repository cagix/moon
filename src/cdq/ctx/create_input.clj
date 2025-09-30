(ns cdq.ctx.create-input
  (:require [cdq.input :as input]))

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (assoc ctx :ctx/input (input/create! input stage)))
