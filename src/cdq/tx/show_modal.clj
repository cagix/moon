(ns cdq.tx.show-modal
  (:require [cdq.stage :as stage]))

(defn do! [[_ opts] {:keys [ctx/stage
                            ctx/ui-viewport]}]
  (stage/show-modal-window! stage ui-viewport opts)
  nil)
