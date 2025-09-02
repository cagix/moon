(ns cdq.tx.show-message
  (:require [cdq.ui.stage :as stage]
            [cdq.ui.message]))

(defn do! [[_ message] ctx]
  (-> ctx
      :ctx/stage
      (stage/find-actor "player-message")
      (cdq.ui.message/show! message))
  nil)
