(ns cdq.tx.show-message
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]))

(defn do! [message]
  (stage/show-message! ctx/stage message))
