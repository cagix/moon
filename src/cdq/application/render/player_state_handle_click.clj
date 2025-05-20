(ns cdq.application.render.player-state-handle-click
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]))

(defn do! []
  (ctx/handle-txs! (state/manual-tick (entity/state-obj @ctx/player-eid)
                                      ctx/player-eid
                                      (ctx/make-map))))
