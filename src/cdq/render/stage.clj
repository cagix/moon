(ns cdq.render.stage
  (:require [clojure.gdx.scene2d.ctx-stage :as ctx-stage]
            [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (ctx-stage/set-ctx! stage ctx)
  (stage/act! stage)
  (stage/draw! stage))
