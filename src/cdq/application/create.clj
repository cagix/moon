(ns cdq.application.create
  (:require cdq.application.create.db
            cdq.application.create.input
            cdq.application.create.graphics
            cdq.application.create.stage
            cdq.application.create.txs
            cdq.application.create.audio
            cdq.application.create.reset-stage
            cdq.application.create.world
            cdq.application.create.reset-world
            cdq.application.create.spawn-player
            cdq.application.create.spawn-enemies
            [cdq.ctx :as ctx]
            [cdq.malli :as m]
            [clojure.disposable :as disposable]
            clojure.gdx.vis-ui
            clojure.tx-handler
            [qrecord.core :as q]))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/app :some]
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/vis-ui :some]
    [:ctx/world :some]]))

(q/defrecord Context [ctx/app
                      ctx/audio
                      ctx/db
                      ctx/graphics
                      ctx/input
                      ctx/stage
                      ctx/vis-ui
                      ctx/world]
  ctx/Validation
  (validate [ctx]
    (m/validate-humanize schema ctx)
    ctx))

(extend-type Context
  ctx/TransactionHandler
  (handle-txs! [ctx transactions]
    (clojure.tx-handler/actions! cdq.application.create.txs/txs-fn-map
                                 ctx
                                 transactions)))

(extend-type Context
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

(defn do! [context]
  (-> (merge (map->Context {})
             context)
      (assoc :ctx/db (cdq.application.create.db/create))
      (assoc :ctx/vis-ui (clojure.gdx.vis-ui/load! {:skin-scale :x1}))
      cdq.application.create.graphics/do!
      cdq.application.create.stage/do!
      cdq.application.create.input/do!
      cdq.application.create.audio/do!
      (dissoc :ctx/files)
      cdq.application.create.world/do!
      (ctx/reset-game-state! "world_fns/vampire.edn")))
