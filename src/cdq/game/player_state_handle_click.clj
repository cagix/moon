(ns cdq.game.player-state-handle-click
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]))

(defn do! []
  (state/manual-tick (entity/state-obj @ctx/player-eid)))
