(ns cdq.render.player-state-handle-click
  (:require [clojure.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]))

(defn do! [{:keys [ctx/player-eid]
            :as ctx}]
  (ctx/handle-txs! ctx
                   (state/manual-tick (entity/state-obj @player-eid)
                                      player-eid
                                      ctx))
  ctx)
