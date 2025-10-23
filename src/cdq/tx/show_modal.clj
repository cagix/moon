(ns cdq.tx.show-modal
  (:require [cdq.ui :as ui]
            [cdq.ui.stage :as stage]))

(defn do!
  [{:keys [ctx/stage] :as ctx} opts]
  (ui/show-modal-window! stage (stage/viewport stage) opts)
  ctx)
