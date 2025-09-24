(ns cdq.application.create
  (:require cdq.create.db
            cdq.create.info
            cdq.create.input
            cdq.create.graphics
            cdq.create.stage
            cdq.create.txs
            cdq.create.audio
            cdq.create.reset-stage
            cdq.create.world
            cdq.create.reset-world
            cdq.create.spawn-player
            cdq.create.spawn-enemies
            [cdq.ctx :as ctx]
            cdq.world-fns.tmx
            [clojure.disposable :as disposable]
            clojure.gdx.vis-ui
            clojure.tx-handler
            [malli.core :as m]
            [malli.utils]
            [qrecord.core :as q]))

(q/defrecord Context [])

(let [schema (m/schema
              [:map {:closed true}
               [:ctx/app :some]
               [:ctx/audio :some]
               [:ctx/db :some]
               [:ctx/graphics :some]
               [:ctx/world :some]
               [:ctx/input :some]
               [:ctx/stage :some]
               [:ctx/vis-ui :some]
               [:ctx/mouseover-actor :any]
               [:ctx/interaction-state :some]])]
  (extend-type Context
    ctx/Validation
    (validate [ctx]
      (malli.utils/validate-humanize schema ctx)
      ctx)))

(extend-type Context
  cdq.ctx/InfoText
  (info-text [ctx entity]
    (cdq.create.info/info-text ctx entity)))

(extend-type Context
  ctx/TransactionHandler
  (handle-txs! [ctx transactions]
    (clojure.tx-handler/actions! cdq.create.txs/txs-fn-map ctx transactions)))

(extend-type Context
  ctx/ResetGameState
  (reset-game-state! [{:keys [ctx/world]
                       :as ctx}
                      world-fn]
    (disposable/dispose! world)
    (-> ctx
        cdq.create.reset-stage/do!
        (cdq.create.reset-world/do! world-fn)
        cdq.create.spawn-player/do!
        cdq.create.spawn-enemies/do!)))

(defn do! [context]
  (-> (merge (map->Context {})
             context)
      (assoc
       :ctx/mouseover-actor nil
       :ctx/interaction-state true)
      (assoc :ctx/db (cdq.create.db/create))
      (assoc :ctx/vis-ui (clojure.gdx.vis-ui/load! {:skin-scale :x1}))
      cdq.create.graphics/do!
      cdq.create.stage/do!
      cdq.create.input/do!
      cdq.create.audio/do!
      (dissoc :ctx/files)
      cdq.create.world/do!
      (ctx/reset-game-state! [cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                                                        :start-position [32 71]}])))
