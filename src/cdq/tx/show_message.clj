(ns cdq.tx.show-message
  (:require [cdq.stage :as stage]))

(defn do! [[_ message]
           {:keys [ctx/stage]}]
  (stage/show-text-message! stage message)
  nil)
