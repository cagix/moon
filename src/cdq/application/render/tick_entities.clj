(ns cdq.application.render.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.render.tick-entities :as tick-entities]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (do (try
         (tick-entities/do! ctx)
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])
           #_(bind-root ::error t)))
        ctx)))
