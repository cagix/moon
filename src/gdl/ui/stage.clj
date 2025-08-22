(ns gdl.ui.stage
  (:require [gdx.ui.actor :as actor]
            [gdx.ui.group :as group])
  (:import (gdl.ui CtxStage)))

; FIXME outdated `.ctx` in the click/etc. listeners as they are processed outside `.act`
(defn render! [^CtxStage stage ctx]
  (reset! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
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
