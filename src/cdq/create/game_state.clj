(ns cdq.create.game-state
  (:require [cdq.content-grid :as content-grid]
            [cdq.g :as g]
            [cdq.game]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :as utils])
  (:import (cdq.game Context)))

(defn- player-entity-props [start-position {:keys [creature-id
                                                   free-skill-points
                                                   click-distance-tiles]}]
  {:position start-position
   :creature-id creature-id
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [{:keys [ctx/stage]} skill]
                                                 (action-bar/add-skill! (:action-bar stage)
                                                                        skill))
                                 :skill-removed! (fn [{:keys [ctx/stage]} skill]
                                                   (action-bar/remove-skill! (:action-bar stage)
                                                                             skill))
                                 :item-set! (fn [{:keys [ctx/stage]} inventory-cell item]
                                              (-> (:windows stage)
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [{:keys [ctx/stage]} inventory-cell]
                                                  (-> (:windows stage)
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position player-props]
  (g/spawn-creature! ctx
                     (player-entity-props (utils/tile->middle start-position)
                                          player-props)))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- create-game-state [{:keys [ctx/config
                                  ctx/stage]
                           :as ctx}
                          world-fn]
  (ui/clear! stage)
  (run! #(ui/add! stage %) ((:create-actors config) ctx))
  (let [{:keys [tiled-map
                start-position]} (world-fn ctx)
        grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create tiled-map (:content-grid-cell-size config))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)
                    :ctx/factions-iterations (:potential-field-factions-iterations config)
                    :ctx/z-orders z-orders
                    :ctx/render-z-order (utils/define-order z-orders)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx
                                                            start-position
                                                            (:player-props config)))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (create-game-state ctx (:world-fn config)))

(extend-type Context
  g/Game
  (reset-game-state! [ctx world-fn]
    (create-game-state ctx world-fn)))
