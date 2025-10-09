(ns cdq.game.create.input-processor
  (:require [clojure.input :as input]))

(defn do!
  [{:keys [ctx/gdx
           ctx/stage]
    :as ctx}]
  (input/set-processor! gdx stage)
  ctx)
