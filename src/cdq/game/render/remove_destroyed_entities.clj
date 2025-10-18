(ns cdq.game.render.remove-destroyed-entities
  (:require [cdq.world :as world]
            [clojure.txs :as txs]))

(defn step
  [{:keys [ctx/world]
    :as ctx}]
  (txs/handle! ctx (world/remove-destroyed-entities! world))
  ctx)
