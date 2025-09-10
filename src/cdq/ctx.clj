(ns cdq.ctx
  (:require [cdq.db :as db]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defn reset-stage!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [config (:cdq.application.reset-game-state config)
        actors (map #(let [[f params] %]
                       ((requiring-resolve f) ctx params))
                    (:create-ui-actors config))]
    (doseq [actor actors]
      (stage/add! stage (actor/build actor))))
  ctx)

(defn call-world-fn
  [{:keys [ctx/db
           ctx/graphics]}
   [f params]]
  ((requiring-resolve f)
   (assoc params
          :creature-properties (db/all-raw db :properties/creatures)
          :graphics graphics)))

(defn reset-game-state!
  [ctx world-fn]
  (-> ctx
      ((requiring-resolve 'cdq.ctx.call-world-fn/do!) (call-world-fn ctx world-fn))
      ((requiring-resolve 'cdq.ctx.build-world/do!))
      ((requiring-resolve 'cdq.ctx.spawn-player/do!))
      ((requiring-resolve 'cdq.ctx.spawn-enemies/do!))))
