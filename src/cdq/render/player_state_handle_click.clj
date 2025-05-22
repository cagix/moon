(ns cdq.render.player-state-handle-click
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]))

(defn do! [{:keys [ctx/player-eid] :as ctx}]
  (g/handle-txs! ctx
                 (state/manual-tick (entity/state-obj @player-eid)
                                    player-eid
                                    ctx))
  nil)
