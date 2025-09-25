(ns gdl.scene2d.stage
  (:import (com.badlogic.gdx.scenes.scene2d StageWithCtx)))

(defn create [viewport batch]
  (StageWithCtx. viewport batch))

(defn set-ctx! [^StageWithCtx stage ctx]
  (set! (.ctx stage) ctx))

(defn get-ctx [^StageWithCtx stage]
  (.ctx stage))

(defn act! [^StageWithCtx stage]
  (.act stage))

(defn draw! [^StageWithCtx stage]
  (.draw stage))

(defn add! [^StageWithCtx stage actor]
  (.addActor stage actor))

(defn clear! [^StageWithCtx stage]
  (.clear stage))

(defn root [^StageWithCtx stage]
  (.getRoot stage))

(defn hit [^StageWithCtx stage [x y]]
  (.hit stage x y true))

(defn viewport [^StageWithCtx stage]
  (.getViewport stage))
