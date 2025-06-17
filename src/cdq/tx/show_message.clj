(ns cdq.tx.show-message
  (:require [cdq.ui.stage :as stage]))

(defn do! [[_ message] ctx]
  (-> ctx
      :ctx/stage
      (stage/show-player-ui-msg! message))
  nil)
