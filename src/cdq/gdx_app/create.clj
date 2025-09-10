(ns cdq.gdx-app.create
  (:require [cdq.ctx :as ctx]))

(defn do! [ctx]
  (let [config (:cdq.gdx-app.create (:ctx/config ctx))]
    ((requiring-resolve (:reset-game-state config))
     (let [ctx ((requiring-resolve (:after-gdx-create config)) ctx)]
       (ctx/reset-stage! ctx)
       ctx)
     (:starting-world config))))
