(ns com.badlogic.gdx.scenes.scene2d.stage
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(defn create [viewport batch]
  (CtxStage. viewport batch))

(defn set-ctx! [^CtxStage stage ctx]
  (set! (.ctx stage) ctx))

(defn get-ctx [^CtxStage stage]
  (.ctx stage))

(defn act! [^CtxStage stage]
  (.act stage))

(defn draw! [^CtxStage stage]
  (.draw stage))

(defn add! [^CtxStage stage actor]
  (.addActor stage actor))

(defn clear! [^CtxStage stage]
  (.clear stage))

(defn root [^CtxStage stage]
  (.getRoot stage))

(defn hit [^CtxStage stage [x y]]
  (.hit stage x y true))

(defn viewport [^CtxStage stage]
  (.getViewport stage))
