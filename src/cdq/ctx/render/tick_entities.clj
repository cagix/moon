(ns cdq.ctx.render.tick-entities
  (:require [gdl.txs :as txs]
            [gdl.throwable :as throwable]
            [cdq.ui :as ui]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (txs/handle! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))
