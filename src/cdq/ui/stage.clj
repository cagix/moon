(ns cdq.ui.stage
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage])
  (:import (clojure.lang ILookup)
           (cdq.ui CtxStage)))

(defn create [viewport batch]
  (proxy [CtxStage ILookup] [viewport batch (atom nil)]
    (valAt [id]
      (group/find-actor-with-id (stage/root this) id))))

(defn get-ctx [^CtxStage stage]
  @(.ctx ^CtxStage stage))

(defn set-ctx! [^CtxStage stage ctx]
  (reset! (.ctx ^CtxStage stage) ctx))

(defn add! [stage actor-or-decl]
  (stage/add! stage (ui/construct? actor-or-decl)))

(def act!   stage/act!)
(def draw!  stage/draw!)
(def clear! stage/clear!)
(def root   stage/root)
(def hit    stage/hit)
