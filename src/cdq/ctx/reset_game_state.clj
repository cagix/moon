(ns cdq.ctx.reset-game-state
  (:require [cdq.ctx.reset-stage-actors :as reset-stage-actors]
            [cdq.ctx.spawn-enemies :as spawn-enemies]
            [cdq.ctx.spawn-player :as spawn-player]
            [cdq.ctx.reset-world-state :as reset-world-state]))

(defn do! [{:keys [ctx/world]
            :as ctx}
           world-fn]
  (-> ctx
      reset-stage-actors/do!
      (reset-world-state/do! world-fn)
      spawn-player/do!
      spawn-enemies/do!))

