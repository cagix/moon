(ns cdq.ui.ctx-stage
  (:require [clojure.gdx.scenes.scene2d.group :as group])
  (:import (clojure.lang ILookup)
           (cdq.ui CtxStage)))

(defn create [viewport batch]
  (proxy [CtxStage ILookup] [viewport batch (atom nil)]
    (valAt [id]
      (group/find-actor-with-id (CtxStage/.getRoot this) id))))

(defn get-ctx [^CtxStage stage]
  @(.ctx ^CtxStage stage))

(defn set-ctx! [^CtxStage stage ctx]
  (reset! (.ctx ^CtxStage stage) ctx))
