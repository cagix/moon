(ns cdq.application.create
  (:require cdq.application.create.effects
            cdq.application.create.entity.state
            cdq.utils
            clojure.edn
            clojure.java.io))

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
    [:cdq.graphics/tiled-map-renderer   (cdq.application.create.tiled-map-renderer/create)]
    [:cdq.graphics/ui-viewport          (cdq.application.create.ui-viewport/create)]
    [:cdq.graphics/world-viewport       (cdq.application.create.world-viewport/create)]
    [:cdq.context/stage                 (cdq.application.create.stage/create)]
    [:cdq.context/elapsed-time          (cdq.application.create.elapsed-time/create)]
    [:cdq.context/player-message        (cdq.application.create.player-message/create)]
    [:cdq.context/level                 (cdq.application.create.level/create)]
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
