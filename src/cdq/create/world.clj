(ns cdq.create.world
  (:require [cdq.ctx :as ctx]))

(defn do! [ctx]
  (ctx/reset-game-state! ctx
                         (:starting-world (:cdq.create.world (:ctx/config ctx)))))
