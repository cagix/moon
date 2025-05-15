(ns cdq.game.player-state-handle-click
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.utils :as utils]))

(defn do! []
  (-> @ctx/player-eid
      entity/state-obj
      state/manual-tick
      utils/handle-txs!))
