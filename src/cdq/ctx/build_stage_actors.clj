(ns cdq.ctx.build-stage-actors
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/stage
           ctx/actor-fns]
    :as ctx}]
  (doseq [[actor-fn & params] actor-fns]
    (stage/add! stage (scene2d/build (apply actor-fn ctx params))))
  ctx)
