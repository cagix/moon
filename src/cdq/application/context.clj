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
            cdq.world.context
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]))

(def context-keyset
  #{:cdq/assets
    :cdq/db
    :cdq/effects
    :cdq/schemas
    :cdq.context/content-grid

    ; delta-time gets added after first frame
    :cdq.context/delta-time

    :cdq.context/elapsed-time
    :cdq.context/entity-ids
    :cdq.context/error
    :cdq.context/explored-tile-corners
    :cdq.context/factions-iterations
    :cdq.context/grid
    :cdq.context/level
    :cdq.context/mouseover-eid
    :cdq.context/paused?
    :cdq.context/player-eid
    :cdq.context/player-message
    :cdq.context/raycaster
    :cdq.context/stage
    :cdq.context/tiled-map
    :cdq.game/active-entities
    :cdq.graphics/batch
    :cdq.graphics/cursors
    :cdq.graphics/default-font
    :cdq.graphics/shape-drawer
    :cdq.graphics/shape-drawer-texture
    :cdq.graphics/tiled-map-renderer
    :cdq.graphics/ui-viewport
    :cdq.graphics/world-unit-scale
    :cdq.graphics/world-viewport
    :context/entity-components
    :world/potential-field-cache}
  )

(comment
 (clojure.pprint/pprint (sort (keys @state)))

 (= context-keyset (set (keys @state)))
 ; => validate each frame !
 ; => after each render/effect ?
 )


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

(defn dispose [context]
  (doseq [[_k value] context
          :when (utils/disposable? value)]
    (utils/dispose value)))

(defn render [context]
  (reduce (fn [context f]
            (f context))
          context
          (for [ns-sym '[cdq.render.assoc-active-entities
                         cdq.render.set-camera-on-player
                         cdq.render.clear-screen
                         cdq.render.tiled-map
                         cdq.render.draw-on-world-view
                         cdq.render.stage
                         cdq.render.player-state-input
                         cdq.render.update-mouseover-entity
                         cdq.render.update-paused
                         cdq.render.when-not-paused

                         ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                         cdq.render.remove-destroyed-entities

                         cdq.render.camera-controls
                         cdq.render.window-controls]]
            (do
             (require ns-sym)
             (resolve (symbol (str ns-sym "/render")))))))

(defn resize [context width height]
  (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
  (viewport/update (:cdq.graphics/world-viewport context) width height))
