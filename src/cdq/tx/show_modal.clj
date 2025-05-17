(ns cdq.tx.show-modal
  (:require [cdq.ctx :as ctx]
            [cdq.ui.modal]
            [gdl.ui.stage :as stage]))

(defn do! [opts]
  (stage/add-actor! ctx/stage (cdq.ui.modal/create opts)))
