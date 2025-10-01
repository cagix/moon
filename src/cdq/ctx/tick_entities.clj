(ns cdq.ctx.tick-entities
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.throwable :as throwable]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (handle-txs/do! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (handle-txs/do! ctx [[:tx/show-error-window t]])))
        ctx)))
