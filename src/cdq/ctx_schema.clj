(ns cdq.ctx-schema
  (:require [cdq.malli :as m]))

; What is used together you can create sub-contexts/objects !
; you see it at the created protocols !
; same with entity !

(def ^:private schema
  (m/schema [:map {:closed true}
             ; audio & graphics
             [:ctx/assets :some]

             ; :ctx/graphics
             [:ctx/batch :some]
             [:ctx/unit-scale :some]
             [:ctx/world-unit-scale :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/shape-drawer :some]
             [:ctx/cursors :some]
             [:ctx/default-font :some]
             [:ctx/world-viewport :some]
             [:ctx/ui-viewport :some]
             [:ctx/tiled-map-renderer :some]

             [:ctx/stage :some]
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

; TODO create here a q/defrecord with all qualified fields
; and pass it to the extenders

(defn validate [ctx]
  (m/validate-humanize schema ctx))
