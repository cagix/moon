(ns cdq.tx.show-modal
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]
            [cdq.ui.modal]))

(defn do! [opts]
  (stage/add-actor! ctx/stage (cdq.ui.modal/create)))
