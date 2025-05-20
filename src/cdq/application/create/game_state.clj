(ns cdq.application.create.game-state
  (:require [cdq.ctx :as ctx]
            cdq.ctx.init-stage
            [cdq.ctx.spawn-enemies :as spawn-enemies]
            [cdq.ctx.spawn-player :as spawn-player]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.utils :refer [bind-root]]
            [gdl.tiled :as tiled]))

(defn reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (cdq.ctx.init-stage/do!)
  (let [{:keys [tiled-map start-position]} ((requiring-resolve world-fn))
        width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    (bind-root #'ctx/start-position start-position)
    (bind-root #'ctx/tiled-map tiled-map)
    (bind-root #'ctx/grid (grid/create tiled-map))
    (bind-root #'ctx/raycaster (raycaster/create ctx/grid))
    (bind-root #'ctx/content-grid (content-grid/create {:cell-size (:content-grid-cells-size ctx/config)
                                                        :width  width
                                                        :height height}))
    (bind-root #'ctx/explored-tile-corners (atom (g2d/create-grid width
                                                                  height
                                                                  (constantly false))))
    (bind-root #'ctx/id-counter (atom 0))
    (bind-root #'ctx/entity-ids (atom {}))
    (bind-root #'ctx/potential-field-cache (atom nil))
    (spawn-enemies/do! {:ctx/tiled-map ctx/tiled-map})
    (spawn-player/do!)))

(defn do! []
  (reset-game! (:tiled-map ctx/config)))
