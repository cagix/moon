(ns cdq.stage-impl
  (:import (com.badlogic.gdx.scenes.scene2d Stage)
           (cdq.ui CtxStage)))

(defn render! [stage ctx]
  (reset! (.ctx ^CtxStage stage) ctx)
  (Stage/.act  stage)
  (Stage/.draw stage)
  ctx)

(defn add! [^Stage stage actor]
  (.addActor stage actor))

(defn clear! [^Stage stage]
  (.clear stage))

(defn root [^Stage stage]
  (.getRoot stage))
