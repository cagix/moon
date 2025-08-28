(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.entity.state.player-moving))

(def ^:private state->handle-input
  {:player-idle           cdq.entity.state.player-idle/handle-input
   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/handle-input
   :player-moving         cdq.entity.state.player-moving/handle-input})

(defn do! [{:keys [ctx/world]
            :as ctx}]
  (let [player-eid (:world/player-eid world)
        handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  ctx)
