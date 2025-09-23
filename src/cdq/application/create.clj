(ns cdq.application.create
  (:require cdq.create.db
            cdq.create.info
            cdq.create.load-entity-states
            cdq.create.load-effects
            cdq.create.input
            cdq.create.graphics
            cdq.create.stage
            cdq.create.set-input-processor
            cdq.create.txs
            cdq.create.audio
            cdq.create.reset-stage
            cdq.create.world
            cdq.create.reset-world
            cdq.create.spawn-player
            cdq.create.spawn-enemies

            [cdq.ctx :as ctx]

            cdq.ui.editor.window
            cdq.world-fns.tmx

            [clojure.disposable :as disposable]
            clojure.gdx.vis-ui

            [clojure.edn :as edn]
            [clojure.java.io :as io]

            [malli.core :as m]
            [malli.utils]

            [qrecord.core :as q]))

(defn actions!
  [txs-fn-map ctx transactions]
  (loop [ctx ctx
         transactions transactions
         handled-transactions []]
    (if (seq transactions)
      (let [[k & params :as transaction] (first transactions)]
        (if transaction
          (let [_ (assert (vector? transaction))
                f (get txs-fn-map k)
                new-transactions (try
                                  (apply f ctx params)
                                  (catch Throwable t
                                    (throw (ex-info "Error handling transaction"
                                                    {:transaction transaction}
                                                    t))))]
            (recur ctx
                   (concat (or new-transactions []) (rest transactions))
                   (conj handled-transactions transaction)))
          (recur ctx
                 (rest transactions)
                 handled-transactions)))
      handled-transactions)))

(defn- edn-resource [path]
  (->> path
       io/resource
       slurp
       edn/read-string))

(q/defrecord Context [])

(let [schema (m/schema
              [:map {:closed true}
               [:ctx/app :some]
               [:ctx/audio :some]
               [:ctx/db :some]
               [:ctx/graphics :some]
               [:ctx/world :some]
               [:ctx/input :some]
               [:ctx/controls :some]
               [:ctx/stage :some]
               [:ctx/vis-ui :some]
               [:ctx/mouseover-actor :any]
               [:ctx/ui-mouse-position :some]
               [:ctx/world-mouse-position :some]
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
    (actions! cdq.create.txs/txs-fn-map ctx transactions)))

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

(cdq.create.load-entity-states/do!)
(cdq.create.load-effects/do!)

(defn do! [context]
  (-> (merge (map->Context {})
             context)
      (assoc
       :ctx/mouseover-actor nil
       :ctx/ui-mouse-position true
       :ctx/world-mouse-position true
       :ctx/interaction-state true)
      (assoc :ctx/db (cdq.create.db/create {:schemas "schema.edn"
                                            :properties "properties.edn"}))
      (assoc :ctx/controls {:zoom-in :minus
                            :zoom-out :equals
                            :unpause-once :p
                            :unpause-continously :space})
      cdq.create.input/do!
      (assoc :ctx/vis-ui (clojure.gdx.vis-ui/load! {:skin-scale :x1}))
      (cdq.create.graphics/do! (edn-resource "graphics.edn"))
      (cdq.create.stage/do!)
      (cdq.create.set-input-processor/do!)
      (cdq.create.audio/do! {:sound-names (edn-resource "sounds.edn")
                             :path-format "sounds/%s.wav"})
      (dissoc :ctx/files)
      (cdq.create.world/do! (edn-resource "world.edn"))
      (ctx/reset-game-state! [cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                                                        :start-position [32 71]}])))
