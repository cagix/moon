(ns cdq.ctx.tick-entities
  (:require [gdl.txs :as txs]
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
         (txs/handle! ctx (tick-entities/do! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))
