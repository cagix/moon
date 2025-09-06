(ns cdq.create.stage
  (:require [clojure.gdx.scenes.scene2d :as scene2d]))

(defn do!
  [{:keys [ctx/ui-viewport
           ctx/batch]
    :as ctx}]
  (assoc ctx :ctx/stage (scene2d/stage ui-viewport batch)))
