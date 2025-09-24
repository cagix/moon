(ns cdq.application.create.reset-game-state
  (:require [cdq.ctx :as ctx]
            [clojure.disposable :as disposable]
            cdq.application.create.reset-stage
            cdq.application.create.reset-world
            cdq.application.create.spawn-player
            cdq.application.create.spawn-enemies))

(defn do! [ctx]
  (extend-type (class ctx)
    ctx/ResetGameState
    (reset-game-state! [{:keys [ctx/world]
                         :as ctx}
                        world-fn]
      (disposable/dispose! world)
      (-> ctx
          cdq.application.create.reset-stage/do!
          (cdq.application.create.reset-world/do! world-fn)
          cdq.application.create.spawn-player/do!
          cdq.application.create.spawn-enemies/do!)))
  ctx)
