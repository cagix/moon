(ns cdq.ctx.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (ctx/handle-txs! ctx (world/tick-entities! world))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))
        ctx)))
