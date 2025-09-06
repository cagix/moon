(ns cdq.create.input-processor
  (:require [clojure.gdx.input :as input]))

(defn do!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (input/set-processor! input stage)
  ctx)
