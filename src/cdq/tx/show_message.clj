(ns cdq.tx.show-message
  (:require [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   message]
  (stage/show-text-message! stage message)
  nil)
