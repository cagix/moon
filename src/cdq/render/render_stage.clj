(ns cdq.render.render-stage
  (:require [clojure.gdx.scenes.scene2d.stage :as stage])
  (:import (cdq.ui CtxStage)))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (reset! (.ctx ^CtxStage stage) ctx)
  (stage/act!  stage)
  (stage/draw! stage))
