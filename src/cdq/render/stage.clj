(ns cdq.render.stage
  (:require [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (reset! (.ctx ^clojure.gdx.scene2d.Stage stage) ctx)
  (stage/act! stage)
  (stage/draw! stage))
