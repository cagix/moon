(ns cdq.application.create
  (:require cdq.application.create.effects
            cdq.application.create.entity.state
            cdq.utils
            clojure.edn
            clojure.java.io))

; TODO side-effects like load visui,set input processor, spawn-enemies not berucksichtigt
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
    [:cdq.context/error                 (cdq.application.create.error/create)]
    [:cdq.context/tiled-map             (cdq.application.create.tiled-map/create)]
    [:cdq.context/explored-tile-corners (cdq.application.create.explored-tile-corners/create)]
    [:cdq.context/grid                  (cdq.application.create.grid/create)]
    [:cdq.context/raycaster             (cdq.application.create.raycaster/create)]
    [:cdq.context/content-grid          (cdq.application.create.content-grid/create)]
    [:cdq.context/entity-ids            (cdq.application.create.entity-ids/create)]
    [:cdq.context/factions-iterations   (cdq.application.create.factions-iterations/create)]
    [:world/potential-field-cache       (cdq.application.create.potential-fields/create)]
    [:cdq.context/player-eid            (cdq.application.create.player-eid/create)]])

(defn context []
  (reduce (fn [context [k fn-invoc]]
            (assoc context k (cdq.utils/req-resolve-call fn-invoc context)))
          {}
          create-components))
