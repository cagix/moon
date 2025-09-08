(ns cdq.application.context.record
  (:require [cdq.malli :as m]
            [qrecord.core :as q]))

(defmacro def-record-and-schema [record-sym & ks]
  `(do

    (q/defrecord ~record-sym
      ~(mapv (comp symbol first) ks))

    (def schema
      [:map {:closed true} ~@ks])

    ))

(def-record-and-schema Context
  [:ctx/active-entities :some]
  [:ctx/audio :some]
  [:ctx/batch :some]
  [:ctx/config :some]
  [:ctx/content-grid :some]
  [:ctx/cursors :some]
  [:ctx/db :some]
  [:ctx/default-font :some]
  [:ctx/delta-time :some]
  [:ctx/draw-fns :some]
  [:ctx/draw-on-world-viewport :some]
  [:ctx/elapsed-time :some]
  [:ctx/entity-ids :some]
  [:ctx/explored-tile-corners :some]
  [:ctx/factions-iterations :some]
  [:ctx/graphics :some]
  [:ctx/grid :some]
  [:ctx/id-counter :some]
  [:ctx/info :some]
  [:ctx/input :some]
  [:ctx/max-delta :some]
  [:ctx/max-speed :some]
  [:ctx/minimum-size :some]
  [:ctx/mouseover-eid :any]
  [:ctx/paused? :any]
  [:ctx/player-eid :some]
  [:ctx/potential-field-cache :some]
  [:ctx/raycaster :some]
  [:ctx/render-layers :some]
  [:ctx/render-z-order :some]
  [:ctx/schema :some]
  [:ctx/shape-drawer :some]
  [:ctx/shape-drawer-texture :some]
  [:ctx/stage :some]
  [:ctx/textures :some]
  [:ctx/tiled-map :some]
  [:ctx/tiled-map-renderer :some]
  [:ctx/ui-actors :some]
  [:ctx/ui-viewport :some]
  [:ctx/unit-scale :some]
  [:ctx/world-unit-scale :some]
  [:ctx/world-viewport :some]
  [:ctx/z-orders :some]

  [:cdq.application.start/pipeline :some]
  [:ctx/os-settings :some]
  [:ctx/lwjgl :some]
  [:ctx/create-fn :some]
  [:ctx/render-pipeline :some]
  [:ctx/dispose-fn :some]
  [:ctx/resize-fn :some]
  [:ctx/application-state :some]
  [:ctx/fsms :some]
  [:ctx/entity-components :some]
  [:ctx/spawn-entity-schema :some]
  [:ctx/render-fn :some]
  [:ctx/reset-game-state-fn :some]
  )

(defn create [ctx]
  (merge (map->Context {:schema (m/schema schema)})
         ctx))
