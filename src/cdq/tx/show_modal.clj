(ns cdq.tx.show-modal
  (:require [cdq.stage :as stage]))

(defn do! [[_ opts] {:keys [ctx/stage]}]
  (stage/show-modal-window! stage (.getViewport stage) opts)
  nil)
