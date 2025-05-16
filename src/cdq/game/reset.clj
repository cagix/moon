(ns cdq.game.reset
  (:require [cdq.ctx :as ctx]
            [cdq.grid2d :as g2d]
            [cdq.impl.stage]
            [cdq.impl.world]
            [cdq.stage :as stage]
            [cdq.state :as state]
            [cdq.utils :as utils :refer [bind-root]]
            [cdq.world.content-grid :as content-grid]
            [gdl.tiled :as tiled]))

(defn- player-entity-props [start-position]
  {:position (utils/tile->middle start-position)
   :creature-id (:creature-id ctx/player-entity-config)
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [skill]
                                                 (stage/add-skill! ctx/stage skill))
                                 :skill-removed! (fn [skill]
                                                   (stage/remove-skill! ctx/stage skill))
                                 :item-set! (fn [inventory-cell item]
                                              (stage/set-item! ctx/stage inventory-cell item))
                                 :item-removed! (fn [inventory-cell]
                                                  (stage/remove-item! ctx/stage inventory-cell))}
                :entity/free-skill-points (:free-skill-points ctx/player-entity-config)
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles (:click-distance-tiles ctx/player-entity-config)}})

(defn- spawn-player [start-position]
  [[:tx/spawn-creature (player-entity-props start-position)]])

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn do! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage (cdq.impl.stage/create!))
  (let [{:keys [tiled-map start-position] :as level} ((requiring-resolve world-fn))
        width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    (bind-root #'ctx/tiled-map tiled-map)
    (bind-root #'ctx/grid (cdq.impl.world/create-grid tiled-map))
    (bind-root #'ctx/raycaster (cdq.impl.world/create-raycaster ctx/grid))
    (bind-root #'ctx/content-grid (content-grid/create {:cell-size 16
                                                        :width  width
                                                        :height height}))
    (bind-root #'ctx/explored-tile-corners (atom (g2d/create-grid width
                                                                  height
                                                                  (constantly false))))
    (bind-root #'ctx/id-counter (atom 0))
    (bind-root #'ctx/entity-ids (atom {}))
    (bind-root #'ctx/potential-field-cache (atom nil))
    (utils/handle-txs! (spawn-enemies tiled-map))
    (utils/handle-txs! (spawn-player start-position))))
