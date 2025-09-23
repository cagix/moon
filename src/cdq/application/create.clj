(ns cdq.application.create
  (:require cdq.create.editor-overview-table
            cdq.create.info
            cdq.create.txs
            cdq.create.load-entity-states
            cdq.create.load-effects
            cdq.create.input
            cdq.create.graphics
            cdq.create.stage
            cdq.create.set-input-processor
            cdq.create.audio
            cdq.create.reset-stage
            cdq.create.world
            cdq.create.reset-world
            cdq.create.spawn-player
            cdq.create.spawn-enemies
            [cdq.ctx :as ctx]
            [cdq.editor]
            cdq.create.db
            cdq.ui.editor.window
            cdq.world-fns.tmx
            clojure.decl
            clojure.gdx.vis-ui
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [clojure.walk :as walk]
            [malli.core :as m]
            [malli.utils]
            [qrecord.core :as q]))

(defn- actions!
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

(defn- create-fn-map [{:keys [ks sym-format]}]
  (into {}
        (for [k ks
              :let [sym (symbol (format sym-format (name k)))
                    f (requiring-resolve sym)]]
          (do
           (assert f (str "Cannot resolve " sym))
           [k f]))))

(defn- require-resolve-symbols [form]
  (if (and (symbol? form)
           (namespace form))
    (let [avar (requiring-resolve form)]
      (assert avar form)
      avar)
    form))

(defn- edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk require-resolve-symbols)))

(cdq.create.load-entity-states/do! (edn-resource "entity_states.edn"))
(cdq.create.load-effects/do!       (edn-resource "effects_fn_map.edn"))

(q/defrecord Context [])

(def ^:private schema
  (m/schema
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
    [:ctx/interaction-state :some]]))

(extend-type Context
  ctx/Validation
  (validate [ctx]
    (malli.utils/validate-humanize schema ctx)
    ctx))

(extend-type Context
  cdq.editor/Editor
  (overview-table-rows [ctx property-type clicked-id-fn]
    (cdq.create.editor-overview-table/create ctx
                                             property-type
                                             clicked-id-fn)))

(extend-type Context
  cdq.ctx/InfoText
  (info-text [ctx entity]
    (cdq.create.info/info-text ctx entity)))

(let [txs-fn-map (create-fn-map (edn-resource "txs.edn"))]
  (extend-type Context
    ctx/TransactionHandler
    (handle-txs! [ctx transactions]
      (actions! txs-fn-map ctx transactions))))

(def ^:private pipeline
  [[(fn [ctx]
      (merge (map->Context {})
             ctx))]

   [assoc
    :ctx/mouseover-actor nil
    :ctx/ui-mouse-position true
    :ctx/world-mouse-position true
    :ctx/interaction-state true]


   [clojure.decl/assoc* :ctx/db [cdq.create.db/create {:schemas "schema.edn"
                                                       :properties "properties.edn"}]]


   [assoc :ctx/controls {:zoom-in :minus
                         :zoom-out :equals
                         :unpause-once :p
                         :unpause-continously :space}]
   [cdq.create.input/do!]

   [clojure.decl/assoc* :ctx/vis-ui [clojure.gdx.vis-ui/load! {:skin-scale :x1}]]

   [cdq.create.graphics/do! (edn-resource "graphics.edn")]

   [cdq.create.stage/do!]

   [cdq.create.set-input-processor/do!]

   [cdq.create.audio/do! {:sound-names (edn-resource "sounds.edn")
                          :path-format "sounds/%s.wav"}]

   [dissoc :ctx/files]

   [cdq.create.reset-stage/do!]

   [cdq.create.world/do! (edn-resource "world.edn")]

   [cdq.create.reset-world/do! [cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                                                          :start-position [32 71]}]]

   [cdq.create.spawn-player/do!]

   [cdq.create.spawn-enemies/do!]
   ])

(defn do! [context]
  (utils/pipeline context pipeline))
