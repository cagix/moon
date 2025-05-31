(ns cdq.create
  (:require [cdq.g :as g]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/config
                      ctx/cursors
                      ctx/db
                      ctx/default-font
                      ctx/graphics
                      ctx/input
                      ctx/stage
                      ctx/ui-viewport
                      ctx/unit-scale
                      ctx/shape-drawer
                      ctx/shape-drawer-texture
                      ctx/tiled-map-renderer
                      ctx/world-unit-scale
                      ctx/world-viewport])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
             [:ctx/batch :some]
             [:ctx/config :some]
             [:ctx/cursors :some]
             [:ctx/db :some]
             [:ctx/default-font :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/stage :some]
             [:ctx/ui-viewport :some]
             [:ctx/unit-scale :some]
             [:ctx/shape-drawer :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/tiled-map-renderer :some]
             [:ctx/world-unit-scale :some]
             [:ctx/world-viewport :some]
             [:ctx/elapsed-time :some]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

(extend-type Context
  g/ContextSchema
  (validate-humanize [ctx]
    (m/validate-humanize schema ctx)))

(defn do! [config]
  (let [ctx (map->Context {:config config})
        ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (map requiring-resolve
                         '[cdq.create.assets/do!
                           cdq.create.input/do!
                           cdq.create.db/do!
                           cdq.create.graphics/do!
                           cdq.create.stage/do!
                           cdq.create.handle-txs/do!
                           cdq.create.game-state/do!
                           cdq.create.info/do!
                           cdq.create.player-movement-vector/do!
                           cdq.create.interaction-state/do!
                           cdq.create.editor/do!
                           ]))]
    (g/validate-humanize ctx)
    ctx))
