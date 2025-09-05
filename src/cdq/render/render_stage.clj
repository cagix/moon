(ns cdq.render.render-stage
  (:require [cdq.ui.ctx-stage :as ctx-stage]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (ctx-stage/set-ctx! stage ctx)
  (stage/act!  stage)
  (stage/draw! stage))
