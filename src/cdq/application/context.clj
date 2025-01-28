(ns cdq.application.context
  (:require [cdq.create.assets :as assets]
            [cdq.create.batch :as batch]
            [cdq.create.cursors :as cursors]
            [cdq.create.default-font :as default-font]
            [cdq.create.db :as db]
            cdq.create.effects
            cdq.create.entity-components
            [cdq.create.schemas :as schemas]
            [cdq.create.shape-drawer :as shape-drawer]
            [cdq.create.shape-drawer-texture :as shape-drawer-texture]
            [cdq.create.stage :as stage]
            [cdq.create.tiled-map-renderer :as tiled-map-renderer]
            [cdq.create.ui-viewport :as ui-viewport]
            [cdq.create.world-unit-scale :as world-unit-scale]
            [cdq.create.world-viewport :as world-viewport]
            cdq.context
            cdq.impl.world.spawn-entity
            cdq.world.context))

(defn create []
  (let [schemas (schemas/create)
        batch (batch/create)
        shape-drawer-texture (shape-drawer-texture/create)
        world-unit-scale (world-unit-scale/create)
        ui-viewport (ui-viewport/create)
        context (cdq.context/map->Context
                 {:cdq/assets (assets/create)
                  :cdq.graphics/batch batch
                  :cdq.graphics/cursors (cursors/create)
                  :cdq.graphics/default-font (default-font/create)
                  :cdq.graphics/shape-drawer (shape-drawer/create batch shape-drawer-texture)
                  :cdq.graphics/shape-drawer-texture shape-drawer-texture
                  :cdq.graphics/tiled-map-renderer (tiled-map-renderer/create batch world-unit-scale)
                  :cdq.graphics/ui-viewport ui-viewport
                  :cdq.graphics/world-unit-scale world-unit-scale
                  :cdq.graphics/world-viewport (world-viewport/create world-unit-scale)
                  :cdq/db (db/create schemas)
                  :context/entity-components (cdq.create.entity-components/create)
                  :cdq/schemas schemas
                  :cdq.context/stage (stage/create batch ui-viewport)}) ]
    (cdq.world.context/reset context :worlds/vampire)))
