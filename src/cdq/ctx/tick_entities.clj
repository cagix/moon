(ns cdq.ctx.tick-entities
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.stage :as stage]
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
           (stage/show-error-window! stage t)))
        ctx)))
