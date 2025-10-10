(ns cdq.world.tx.show-modal
  (:require [cdq.ui :as ui]))

(defn do! [{:keys [ctx/stage]} opts]
  (ui/show-modal-window! stage (.getViewport stage) opts)
  nil)
