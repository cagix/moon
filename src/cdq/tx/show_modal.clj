(ns cdq.tx.show-modal
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]))

(defn do! [opts]
  (stage/show-modal! ctx/stage opts))
