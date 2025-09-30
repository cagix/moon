(ns cdq.c
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.reset-stage-actors :as reset-stage-actors]
            [cdq.ctx.spawn-enemies :as spawn-enemies]
            [cdq.ctx.spawn-player :as spawn-player]
            [cdq.ctx.reset-world-state :as reset-world-state]
            [com.badlogic.gdx.utils.disposable :as disposable]))

(defn create! [ctx]
  (extend-type (class ctx)
    ctx/ResetGameState
    (reset-game-state! [{:keys [ctx/world]
                         :as ctx}
                        world-fn]
      (disposable/dispose! world)
      (-> ctx
          reset-stage-actors/do!
          (reset-world-state/do! world-fn)
          spawn-player/do!
          spawn-enemies/do!)) )
  (-> ctx
      (ctx/reset-game-state! "world_fns/vampire.edn")))
