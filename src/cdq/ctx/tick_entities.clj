(ns cdq.ctx.tick-entities
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.throwable :as throwable]
            [cdq.ui :as ui]
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
           (ui/show-error-window! stage t)))
        ctx)))
