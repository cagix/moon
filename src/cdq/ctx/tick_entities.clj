(ns cdq.ctx.tick-entities
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.stage.error-window :as error-window]
            [cdq.throwable :as throwable]
            [cdq.world.tick-entities :as tick-entities]))

(defn do!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (handle-txs/do! ctx (tick-entities/do! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (error-window/show! stage t)))
        ctx)))
