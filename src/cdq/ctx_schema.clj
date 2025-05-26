(ns cdq.ctx-schema
  (:require [cdq.malli :as m]))


; 0. Move cdq.context and gdl.context together
; => its _pure wiring_
; => expose gdl objects independently , e.g. assets

; 1. create the q/defrecord with all fields here
; 2. extend step by step and see which 'ctx/' keys are used together -> ?
; => use gdl.utils/create-into!

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
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

(defn validate [ctx]
  (m/validate-humanize schema ctx))
