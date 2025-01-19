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
  '[[:cdq/schemas (cdq.application.create.schemas/create)]
    [:cdq/db      (cdq.application.create.db/create)]
    [:cdq/assets   (cdq.impl.assets/manager)]
    [:cdq.graphics/batch (cdq.gdx.graphics/sprite-batch)]
    [:cdq.graphics/shape-drawer-texture (cdq.graphics/white-pixel-texture)]
    [:cdq.graphics/shape-drawer (cdq.gdx.graphics.shape-drawer/create)]
    [:cdq.graphics/cursors (cdq.graphics/cursors {:cursors/bag                   ["bag001"       [0   0]]
                                                  :cursors/black-x               ["black_x"      [0   0]]
                                                  :cursors/default               ["default"      [0   0]]
                                                  :cursors/denied                ["denied"       [16 16]]
                                                  :cursors/hand-before-grab      ["hand004"      [4  16]]
                                                  :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                                  :cursors/hand-grab             ["hand003"      [4  16]]
                                                  :cursors/move-window           ["move002"      [16 16]]
                                                  :cursors/no-skill-selected     ["denied003"    [0   0]]
                                                  :cursors/over-button           ["hand002"      [0   0]]
                                                  :cursors/sandclock             ["sandclock"    [16 16]]
                                                  :cursors/skill-not-usable      ["x007"         [0   0]]
                                                  :cursors/use-skill             ["pointer004"   [0   0]]
                                                  :cursors/walking               ["walking"      [16 16]]})]
    [:cdq.graphics/default-font (cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                   :size 16
                                                                   :quality-scaling 2})]
    [:cdq.graphics/world-unit-scale (cdq.graphics.world-unit-scale/create 48)]
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
