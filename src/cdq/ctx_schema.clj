(ns cdq.ctx-schema
  (:require [cdq.malli :as m]))

(def ^:private schema
  (m/schema [:map {:closed true}
             ; audio & graphics
             [:assets :some]

             ; :ctx/graphics
             [:batch :some]
             [:unit-scale :some]
             [:world-unit-scale :some]
             [:shape-drawer-texture :some]
             [:shape-drawer :some]
             [:cursors :some]
             [:default-font :some]
             [:world-viewport :some]
             [:ui-viewport :some]
             [:tiled-map-renderer :some]

             [:stage :some]
             [:ctx/config :some]
             [:ctx/db :some]
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
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

(defn validate [ctx]
  (m/validate-humanize schema ctx))
