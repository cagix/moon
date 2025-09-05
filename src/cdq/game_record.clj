(ns cdq.game-record
  (:require [qrecord.core :as q]
            [cdq.malli :as m]))

; TODO macro ? 38x duplication

(q/defrecord Context
  [
   ctx/active-entities
   ctx/audio
   ctx/batch
   ctx/config
   ctx/content-grid
   ctx/cursors
   ctx/db
   ctx/default-font
   ctx/delta-time
   ctx/elapsed-time
   ctx/entity-ids
   ctx/explored-tile-corners
   ctx/factions-iterations
   ctx/graphics
   ctx/grid
   ctx/id-counter
   ctx/input
   ctx/max-delta
   ctx/max-speed
   ctx/minimum-size
   ctx/mouseover-eid
   ctx/paused?
   ctx/player-eid
   ctx/potential-field-cache
   ctx/raycaster
   ctx/render-z-order
   ctx/schema
   ctx/shape-drawer
   ctx/shape-drawer-texture
   ctx/stage
   ctx/textures
   ctx/tiled-map
   ctx/tiled-map-renderer
   ctx/ui-viewport
   ctx/unit-scale
   ctx/world-unit-scale
   ctx/world-viewport
   ctx/z-orders
   ])

(def ^:private schema
  [:map {:closed true}
   [:ctx/active-entities :some]
   [:ctx/audio :some]
   [:ctx/batch :some]
   [:ctx/config :some]
   [:ctx/content-grid :some]
   [:ctx/cursors :some]
   [:ctx/db :some]
   [:ctx/default-font :some]
   [:ctx/delta-time :some]
   [:ctx/elapsed-time :some]
   [:ctx/entity-ids :some]
   [:ctx/explored-tile-corners :some]
   [:ctx/factions-iterations :some]
   [:ctx/graphics :some]
   [:ctx/grid :some]
   [:ctx/id-counter :some]
   [:ctx/input :some]
   [:ctx/max-delta :some]
   [:ctx/max-speed :some]
   [:ctx/minimum-size :some]
   [:ctx/mouseover-eid :any]
   [:ctx/paused? :any]
   [:ctx/player-eid :some]
   [:ctx/potential-field-cache :some]
   [:ctx/raycaster :some]
   [:ctx/render-z-order :some]
   [:ctx/schema :some]
   [:ctx/shape-drawer :some]
   [:ctx/shape-drawer-texture :some]
   [:ctx/stage :some]
   [:ctx/textures :some]
   [:ctx/tiled-map :some]
   [:ctx/tiled-map-renderer :some]
   [:ctx/ui-viewport :some]
   [:ctx/unit-scale :some]
   [:ctx/world-unit-scale :some]
   [:ctx/world-viewport :some]
   [:ctx/z-orders :some]
   ])

(defn create-with-schema []
  (map->Context {:schema (m/schema schema)}))
