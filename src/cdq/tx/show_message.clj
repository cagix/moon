(ns cdq.tx.show-message
  (:require [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.ui.message]))

(defn do!
  [[_ message]
   {:keys [ctx/stage]}]
  (-> stage
      stage/root
      (group/find-actor "player-message")
      (cdq.ui.message/show! message))
  nil)
