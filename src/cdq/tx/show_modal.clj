(ns cdq.tx.show-modal
  (:require [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   opts]
  (stage/show-modal-window! stage (.getViewport stage) opts)
  nil)
