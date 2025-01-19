(ns cdq.application.create
  (:require cdq.application.create.effects
            cdq.application.create.entity.state
            cdq.utils
            clojure.edn
            clojure.java.io))

; schemas (this) is a map
; which supports certain operations (get, validate, etc.) ...
; so it all definition should be here probably , with malli and primitives etc

(def create-components
  '[[:cdq/schemas                       (cdq.application.create.schemas/create)]
    [:cdq/db                            (cdq.application.create.db/create)]
    [:cdq/assets                        (cdq.application.create.assets/create)]
    [:cdq.graphics/batch                (cdq.application.create.batch/create)]
    [:cdq.graphics/shape-drawer-texture (cdq.application.create.shape-drawer-texture/create)]
    [:cdq.graphics/shape-drawer         (cdq.application.create.shape-drawer/create)]
    [:cdq.graphics/cursors              (cdq.application.create.cursors/create)]
    [:cdq.graphics/default-font         (cdq.application.create.default-font/create)]
    [:cdq.graphics/world-unit-scale     (cdq.application.create.world-unit-scale/create)]
    [:cdq.graphics/tiled-map-renderer (cdq.graphics.tiled-map-renderer/create)]
    [:cdq.graphics/ui-viewport (cdq.graphics.ui-viewport/create {:width 1440 :height 900})]
    [:cdq.graphics/world-viewport (cdq.graphics.world-viewport/create {:width 1440 :height 900})]
    [:cdq.context/stage (cdq.ui/setup-stage! {:skin-scale :x1
                                              :actors [(cdq.ui.dev-menu/create)
                                                       (cdq.ui.actionbar/create)
                                                       (cdq.ui.hp-mana-bar/create)
                                                       (cdq.ui.windows/create [(cdq.ui.entity-info-window/create)
                                                                               (cdq.widgets.inventory/create)])
                                                       (cdq.ui.player-state/create)
                                                       (cdq.ui.player-message/actor)]})]
    [:cdq.context/elapsed-time (cdq.timer/create-ctx)]
    [:cdq.context/player-message (cdq.ui.player-message/create* {:duration-seconds 1.5})]
    [:cdq.context/level (cdq.level/create :worlds/uf-caves)]
    [:cdq.context/error (cdq.create/error*)]
    [:cdq.context/tiled-map (cdq.create/tiled-map*)]
    [:cdq.context/explored-tile-corners (cdq.world.explored-tile-corners/create)]
    [:cdq.context/grid (cdq.world.grid/create)]
    [:cdq.context/raycaster (cdq.world.raycaster/create)]
    [:cdq.context/content-grid (cdq.world.content-grid/create {:cell-size 16})]
    [:cdq.context/entity-ids (cdq.create/entity-ids*)]
    [:cdq.context/factions-iterations (cdq.create/factions-iterations* {:good 15 :evil 5})]
    [:world/potential-field-cache (cdq.potential-fields/create-cache)]
    [:cdq.context/player-eid (cdq.world.entities/spawn)]])

(defn context []
  (reduce (fn [context [k fn-invoc]]
            (assoc context k (cdq.utils/req-resolve-call fn-invoc context)))
          {}
          create-components))
