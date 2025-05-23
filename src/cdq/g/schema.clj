(ns cdq.g.schema
  (:require [cdq.g :as g]
            [cdq.malli :as m]))

(def schema (m/schema [:map {:closed true}

                       [:ctx/config :some] ; => simple get API - will throw exception if key is not found - plain data ?
                       [:ctx/db :some] ; -> using internal db API -> move into ctx?

                       [:ctx/assets :some] ; => simple get API - ...

                       ; => make graphics object ??
                       [:ctx/batch :some]
                       [:ctx/shape-drawer-texture :some]
                       [:ctx/shape-drawer :some]
                       [:ctx/unit-scale :some]
                       [:ctx/world-unit-scale :some]
                       [:ctx/cursors :some]
                       [:ctx/default-font :some]
                       [:ctx/world-viewport :some]
                       [:ctx/tiled-map-renderer :some]
                       [:ctx/ui-viewport :some] ; to stage ?
                       ; vimgrep/ctx\/\(batch\|shape-drawer\|unit-scale\|world-\|cursors\|default-f\|tiled-map-rend\|ui-viewport\)/g src/** test/**

                       [:ctx/stage :some]

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
                       [:ctx/active-entities {:optional true} :some]

                       ]))

(extend-type cdq.g.Game
  g/Schema
  (validate [ctx]
    (m/validate-humanize schema ctx)))
