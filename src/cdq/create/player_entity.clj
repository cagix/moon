(ns cdq.create.player-entity
  (:require [cdq.ctx :as ctx]
            [cdq.state :as state]
            [cdq.tx.spawn-creature]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.utils :as utils]))

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
                                                 (-> stage
                                                     :action-bar
                                                     (action-bar/add-skill! skill)))
                                 :skill-removed! (fn [{:keys [ctx/stage]} skill]
                                                   (-> stage
                                                       :action-bar
                                                       (action-bar/remove-skill! skill)))
                                 :item-set! (fn [{:keys [ctx/stage]} inventory-cell item]
                                              (-> stage
                                                  :windows
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [{:keys [ctx/stage]} inventory-cell]
                                                  (-> stage
                                                      :windows
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn do! [{:keys [ctx/start-position] :as ctx}]
  (cdq.tx.spawn-creature/do! ctx
                             (player-entity-props (utils/tile->middle start-position)
                                                  ctx/player-entity-config)))
