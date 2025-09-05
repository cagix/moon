(ns cdq.ui.ctx-stage
  (:require [cdq.ui.group :as group])
  (:import (clojure.lang ILookup)
           (cdq.ui CtxStage)))

(defn create [viewport batch]
  (proxy [CtxStage ILookup] [viewport batch (atom nil)]
    (valAt [id]
      (group/find-actor-with-id (CtxStage/.getRoot this) id))))
