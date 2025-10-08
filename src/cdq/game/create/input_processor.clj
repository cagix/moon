(ns cdq.game.create.input-processor
  (:require [clojure.gdx :as gdx]))

(defn do!
  [{:keys [ctx/gdx
           ctx/stage]
    :as ctx}]
  (gdx/set-input-processor! gdx stage)
  ctx)
