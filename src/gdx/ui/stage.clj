(ns gdx.ui.stage
  (:require [gdx.ui.actor :as actor]
            [gdx.ui.group :as group])
  (:import (gdl.ui CtxStage)))

(defn render! [^CtxStage stage ctx]
  (reset! (.ctx stage) ctx)
  (.act stage)
  ; We cannot pass this
  ; because input events are handled outside ui/act! and in the Lwjgl3Input system
  #_@(.ctx (-k ctx))
  ; we need to set nil as input listeners
  ; are updated outside of render
  ; inside lwjgl3application code
  ; FIXME so it has outdated context.
  #_(reset! (.ctx (-k ctx)) nil)
  (reset! (.ctx stage) ctx)
  (.draw stage)
  ; we need to set nil as input listeners
  ; are updated outside of render
  ; inside lwjgl3application code
  ; so it has outdated context
  ; => maybe context should be an immutable data structure with mutable fields?
  #_(reset! (.ctx (-k ctx)) nil)
  ctx)

(defn add! [^CtxStage stage actor-or-decl]
  (.addActor stage (actor/construct? actor-or-decl)))

(defn clear! [^CtxStage stage]
  (.clear stage))

(defn hit [^CtxStage stage [x y]]
  (.hit stage x y true))

(defn find-actor [^CtxStage stage actor-name]
  (-> stage
      .getRoot
      (group/find-actor actor-name)))
