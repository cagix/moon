(ns cdq.create.world
  (:require cdq.application.reset-game-state))

(defn do! [ctx]
  (cdq.application.reset-game-state/reset-game-state! ctx
                                                      (:starting-world (:cdq.create.world (:ctx/config ctx)))))
