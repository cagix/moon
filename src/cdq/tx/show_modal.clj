(ns cdq.tx.show-modal
  (:require [cdq.ctx :as ctx]
            [cdq.ui.modal]
            [gdl.ui :as ui]))

(defn do! [opts]
  (ui/add! ctx/stage (cdq.ui.modal/create opts)))
