(ns cdq.tx.show-modal
  (:require [cdq.stage :as stage]
            [gdl.scene2d.stage]))

(defn do!
  [{:keys [ctx/stage]}
   opts]
  (stage/show-modal-window! stage (gdl.scene2d.stage/viewport stage) opts)
  nil)
