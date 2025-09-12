(ns cdq.render.stage
  (:require [clojure.gdx.scenes.scene2d.stage]
            [clojure.scenes.scenes2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (clojure.gdx.scenes.scene2d.stage/set-ctx! stage ctx)
  (stage/act! stage)
  (stage/draw! stage))
