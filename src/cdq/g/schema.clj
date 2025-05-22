(ns cdq.g.schema
  (:require [cdq.g :as g]
            [cdq.malli :as m]))

(def ctx-schema (m/schema [:map {:closed true}
                           [:ctx/pausing? :some]
                           [:ctx/zoom-speed :some]
                           [:ctx/controls :some]
                           [:ctx/sound-path-format :some]
                           [:ctx/effect-body-props :some]
                           [:ctx/config :some]
                           [:ctx/db :some]
                           [:ctx/assets :some]
                           [:ctx/batch :some]
                           [:ctx/shape-drawer-texture :some]
                           [:ctx/shape-drawer :some]
                           [:ctx/unit-scale :some]
                           [:ctx/world-unit-scale :some]
                           [:ctx/cursors :some]
                           [:ctx/default-font :some]
                           [:ctx/world-viewport :some]
                           [:ctx/tiled-map-renderer :some]
                           [:ctx/ui-viewport :some]
                           [:ctx/stage :some]
                           [:ctx/elapsed-time :some]
                           [:ctx/delta-time {:optional true} number?] ; optional - added in render each frame
                           [:ctx/paused? {:optional true} :boolean] ; optional - added in render each frame
                           [:ctx/tiled-map :some]
                           [:ctx/grid :some]
                           [:ctx/raycaster :some]
                           [:ctx/content-grid :some]
                           [:ctx/explored-tile-corners :some]
                           [:ctx/id-counter :some]
                           [:ctx/entity-ids :some]
                           [:ctx/potential-field-cache :some]
                           [:ctx/mouseover-eid :any]
                           [:ctx/player-eid :some]
                           [:ctx/active-entities {:optional true} :some] ; optional - added in render each frame
                           ]))

(extend-type cdq.g.Game
  g/Schema
  (validate [ctx]
    (m/validate-humanize ctx-schema ctx)))
