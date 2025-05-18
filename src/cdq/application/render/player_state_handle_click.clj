(ns cdq.application.render.player-state-handle-click
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [handle-txs!]]))

(defn do! []
  (-> @ctx/player-eid
      entity/state-obj
      state/manual-tick
      handle-txs!))
