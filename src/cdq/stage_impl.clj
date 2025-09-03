(ns cdq.stage-impl
  (:require [cdq.ui.actor :as actor])
  (:import (cdq.ui CtxStage)))

; this act draw also makese no sense

; FIXME outdated `.ctx` in the click/etc. listeners as they are processed outside `.act`
(defn render! [^CtxStage stage ctx]
  (reset! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
  ctx)

(defn add! [^CtxStage stage actor-or-decl]
  (.addActor stage (actor/construct? actor-or-decl))) ; this doesnt make any sense, just pass an actor

(defn clear! [^CtxStage stage]
  (.clear stage))

(defn root [^CtxStage stage]
  (.getRoot stage))
