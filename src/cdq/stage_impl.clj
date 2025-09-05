(ns cdq.stage-impl
  (:import (com.badlogic.gdx.scenes.scene2d Stage)
           (cdq.ui CtxStage)))

(defn render! [stage ctx]
  (reset! (.ctx ^CtxStage stage) ctx)
  (Stage/.act  stage)
  (Stage/.draw stage))

(defn add! [^Stage stage actor]
  (.addActor stage actor))

(defn clear! [^Stage stage]
  (.clear stage))

(defn root [^Stage stage]
  (.getRoot stage))

(defn hit [stage [x y]]
  (Stage/.hit stage x y true))
